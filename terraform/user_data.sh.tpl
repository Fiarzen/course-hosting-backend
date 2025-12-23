#!/bin/bash

# Basic updates
apt-get update -y || yum update -y || true

# Install Java if needed (Ubuntu / Amazon Linux examples)
if command -v apt-get >/dev/null 2>&1; then
  apt-get install -y openjdk-17-jre-headless awscli
elif command -v yum >/dev/null 2>&1; then
  yum install -y java-17-amazon-corretto-headless awscli || yum install -y java-17-openjdk awscli
fi

# Create app directory
mkdir -p /opt/course-app
cd /opt/course-app

# Download the Spring Boot JAR from S3 using the instance's IAM role
aws s3 cp "s3://${artifact_bucket}/${artifact_key}" /opt/course-app/app.jar
chmod 755 /opt/course-app/app.jar

# Create systemd service file
cat << 'EOF' > /etc/systemd/system/course-app.service
[Unit]
Description=Course Hosting Spring Boot Application
After=network.target

[Service]
User=root
WorkingDirectory=/opt/course-app
Environment="DATABASE_URL=jdbc:postgresql://${db_host}:${db_port}/${db_name}"
Environment="DATABASE_USERNAME=${db_username}"
Environment="DATABASE_PASSWORD=${db_password}"
Environment="S3_BUCKET_NAME=${s3_bucket_name}"
ExecStart=/usr/bin/java -jar /opt/course-app/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and start service
systemctl daemon-reload
systemctl enable course-app.service
systemctl start course-app.service
