#!/bin/bash
set -e

###################################
# OS Updates & Dependencies
###################################
if command -v apt-get >/dev/null 2>&1; then
  apt-get update -y
  apt-get install -y openjdk-17-jre-headless awscli
elif command -v yum >/dev/null 2>&1; then
  yum update -y
  yum install -y java-17-amazon-corretto-headless awscli \
    || yum install -y java-17-openjdk awscli
fi

###################################
# Application Directory
###################################
mkdir -p /opt/course-app
chown root:root /opt/course-app
chmod 755 /opt/course-app

###################################
# systemd Service
###################################
cat << EOF > /etc/systemd/system/course-app.service
[Unit]
Description=Course Hosting Spring Boot Application
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/course-app

# --- Environment Variables ---
Environment="DATABASE_URL=jdbc:postgresql://${db_host}:${db_port}/${db_name}"
Environment="DATABASE_USERNAME=${db_username}"
Environment="DATABASE_PASSWORD=${db_password}"
Environment="S3_BUCKET_NAME=${s3_bucket_name}"
Environment="APP_ADMIN_EMAIL=${app_admin_email}"
Environment="APP_ADMIN_PASSWORD=${app_admin_password}"
Environment="APP_STUDENT_EMAIL=${app_student_email}"
Environment="APP_STUDENT_PASSWORD=${app_student_password}"

# --- Always pull latest JAR before start ---
ExecStartPre=/usr/bin/aws s3 cp s3://${artifact_bucket}/${artifact_key} /opt/course-app/app.jar
ExecStart=/usr/bin/java -jar /opt/course-app/app.jar

Restart=always
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

###################################
# Start Service
###################################
systemctl daemon-reload
systemctl enable course-app
systemctl start course-app
