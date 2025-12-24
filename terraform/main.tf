terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# -----------------------------
# S3 bucket for lesson files
# -----------------------------
resource "aws_s3_bucket" "lesson_files" {
  bucket = var.s3_bucket_name

  tags = {
    Project = "course-hosting-backend"
    Env     = var.environment
  }
}

resource "aws_s3_bucket_public_access_block" "lesson_files" {
  bucket = aws_s3_bucket.lesson_files.id

  block_public_acls       = true
  block_public_policy     = false
  ignore_public_acls      = true
  restrict_public_buckets = false
}

# Optional: allow public read of objects (for PDFs/videos) via bucket policy
resource "aws_s3_bucket_policy" "lesson_files_public" {
  bucket = aws_s3_bucket.lesson_files.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"
        Action    = ["s3:GetObject"]
        Resource  = ["${aws_s3_bucket.lesson_files.arn}/*"]
      }
    ]
  })
}

# CORS so browser apps can access files
resource "aws_s3_bucket_cors_configuration" "lesson_files" {
  bucket = aws_s3_bucket.lesson_files.id

  cors_rule {
    allowed_methods = ["GET"]
    allowed_origins = ["*"]
    allowed_headers = ["*"]
    max_age_seconds = 3000
  }
}

# -----------------------------
# S3 bucket for artifacts (Spring Boot JAR)
# -----------------------------
resource "aws_s3_bucket" "artifacts" {
  bucket = var.artifact_bucket_name

  tags = {
    Project = "course-hosting-backend"
    Env     = var.environment
    Type    = "artifacts"
  }
}

resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}


# Upload the built JAR to the artifact bucket
resource "aws_s3_object" "artifact_jar" {
  bucket = aws_s3_bucket.artifacts.id
  key    = var.artifact_key
  source = var.artifact_source_path
  etag   = filemd5(var.artifact_source_path)
}

# Convenience locals for user_data interpolation
locals {
  app_admin_email       = var.app_admin_email
  app_admin_password    = var.app_admin_password
  app_student_email     = var.app_student_email
  app_student_password  = var.app_student_password
}

# -----------------------------
# Security groups
# -----------------------------
resource "aws_security_group" "app_sg" {
  name        = "course-app-sg"
  description = "Allow HTTP and SSH to app server"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP from anywhere"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH from anywhere (change this in production!)"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "course-app-sg"
    Project = "course-hosting-backend"
    Env     = var.environment
  }
}

resource "aws_security_group" "db_sg" {
  name        = "course-db-sg"
  description = "Allow Postgres access from app server"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description      = "Postgres from app SG"
    from_port        = 5432
    to_port          = 5432
    protocol         = "tcp"
    security_groups  = [aws_security_group.app_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "course-db-sg"
    Project = "course-hosting-backend"
    Env     = var.environment
  }
}

# -----------------------------
# Use default VPC & subnets (simpler for demo)
# -----------------------------
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# -----------------------------
# EC2 key pair for SSH access
# -----------------------------
resource "aws_key_pair" "app_key" {
  key_name   = var.ssh_key_pair_name
  public_key = file(var.ssh_public_key_path)
}

# -----------------------------
# IAM role for EC2 to read artifact JAR from S3
# -----------------------------
resource "aws_iam_role" "app_role" {
  name = "course-app-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "app_s3_artifact" {
  name = "course-app-s3-artifact-policy"
  role = aws_iam_role.app_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:GetObject"],
        Resource = ["${aws_s3_bucket.artifacts.arn}/${var.artifact_key}"]
      }
    ]
  })
}

resource "aws_iam_role_policy" "app_s3_lesson_files" {
  name = "course-app-s3-lesson-files-policy"
  role = aws_iam_role.app_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject"
        ]
        Resource = ["${aws_s3_bucket.lesson_files.arn}/*"]
      }
    ]
  })
}

resource "aws_iam_instance_profile" "app_profile" {
  name = "course-app-instance-profile"
  role = aws_iam_role.app_role.name
}

# -----------------------------
# RDS PostgreSQL instance
# -----------------------------
resource "aws_db_subnet_group" "db_subnets" {
  name       = "course-db-subnets"
  subnet_ids = data.aws_subnets.default.ids

  tags = {
    Name    = "course-db-subnet-group"
    Project = "course-hosting-backend"
    Env     = var.environment
  }
}

resource "aws_db_instance" "postgres" {
  identifier        = "course-db"
  engine            = "postgres"
  # Let AWS use the default supported engine version for this region
  instance_class    = var.db_instance_class
  allocated_storage = 20

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.db_subnets.name
  vpc_security_group_ids = [aws_security_group.db_sg.id]

  skip_final_snapshot = true

  publicly_accessible = false

  deletion_protection = false

  tags = {
    Name    = "course-db"
    Project = "course-hosting-backend"
    Env     = var.environment
  }
}

data "aws_ami" "app_ami" { 
  
  most_recent = true 
  owners = ["099720109477"]
  filter { 
    name = "name" 
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"] 
  } 
}

# -----------------------------
# EC2 instance for Spring Boot app
# -----------------------------
resource "aws_instance" "app" {
  ami                    = data.aws_ami.app_ami.id
  instance_type          = var.app_instance_type
  subnet_id              = data.aws_subnets.default.ids[0]
  vpc_security_group_ids = [aws_security_group.app_sg.id]

  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.app_profile.name
  key_name                    = aws_key_pair.app_key.key_name

  user_data = templatefile("${path.module}/user_data.sh.tpl", {
    artifact_bucket      = aws_s3_bucket.artifacts.bucket
    artifact_key         = var.artifact_key
    db_host              = aws_db_instance.postgres.address
    db_port              = aws_db_instance.postgres.port
    db_name              = var.db_name
    db_username          = var.db_username
    db_password          = var.db_password
    s3_bucket_name       = aws_s3_bucket.lesson_files.bucket
    app_admin_email      = local.app_admin_email
    app_admin_password   = local.app_admin_password
    app_student_email    = local.app_student_email
    app_student_password = local.app_student_password
  })

  tags = {
    Name    = "course-app-server"
    Project = "course-hosting-backend"
    Env     = var.environment
  }
}

# -----------------------------
# Elastic IP for the app instance (stable public IP)
# -----------------------------
resource "aws_eip" "app_eip" {
  domain   = "vpc"
  instance = aws_instance.app.id

  tags = {
    Name    = "course-app-eip"
    Project = "course-hosting-backend"
    Env     = var.environment
  }
}
