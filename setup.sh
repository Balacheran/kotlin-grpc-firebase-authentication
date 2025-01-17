# setup.sh
#!/bin/bash

if [ ! -f .env ]; then
    echo "Creating .env file..."
    cp .env.example .env
    echo "Please edit .env file and add your Firebase Web API Key"
else
    echo ".env file already exists"
fi
