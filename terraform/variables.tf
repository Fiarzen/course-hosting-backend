variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Deployment environment name"
  type        = string
  default     = "dev"
}

variable "s3_bucket_name" {
  description = "Name of S3 bucket for lesson files"
  type        = string
  default     = "course-hosting-lesson-files-dev"
}

variable "db_instance_class" {
  description = "Instance class for RDS PostgreSQL"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Database name for the app"
  type        = string
  default     = "coursedb"
}

variable "db_username" {
  description = "Master username for PostgreSQL"
  type        = string
  default     = "course_user"
}

variable "db_password" {
  description = "Master password for PostgreSQL"
  type        = string
  sensitive   = true
}

variable "app_ami_id" {
  description = "AMI ID for the application EC2 instance (use an Ubuntu or Amazon Linux image)"
  type        = string
}

variable "app_instance_type" {
  description = "EC2 instance type for the app server"
  type        = string
  default     = "t3.micro"
}
