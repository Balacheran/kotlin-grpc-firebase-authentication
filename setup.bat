@echo off
if not exist .env (
    echo Creating .env file...
    copy .env.example .env
    echo Please edit .env file and add your Firebase Web API Key
) else (
    echo .env file already exists
)
pause