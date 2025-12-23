variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "eu-west-1"
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

variable "ssh_public_key_path" {
  description = "Path to local SSH public key to use for EC2"
  type        = string
  default     = "~/.ssh/id_ed25519.pub"
}

variable "ssh_key_pair_name" {
  description = "Name for the EC2 key pair"
  type        = string
  default     = "course-hosting-key"
}

variable "artifact_bucket_name" {
  description = "Name of S3 bucket that stores the built Spring Boot JAR"
  type        = string
  default     = "course-hosting-artifacts-dev"
}

variable "artifact_key" {
  description = "Object key (path) of the Spring Boot JAR within the artifact bucket"
  type        = string
  default     = "courses.jar"
}

variable "artifact_source_path" {
  description = "Local path to the built Spring Boot JAR to upload to S3"
  type        = string
  default     = "../courses/target/courses-0.0.1-SNAPSHOT.jar"
}
