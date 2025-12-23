output "app_public_ip" {
  description = "Public IP of the application EC2 instance"
  value       = aws_instance.app.public_ip
}

output "rds_endpoint" {
  description = "RDS endpoint for PostgreSQL"
  value       = aws_db_instance.postgres.address
}

output "s3_bucket_name" {
  description = "S3 bucket for lesson files"
  value       = aws_s3_bucket.lesson_files.bucket
}
