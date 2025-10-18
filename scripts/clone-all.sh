#!/bin/bash

# --- BACKEND ---
mkdir -p backend
cd backend
git clone https://github.com/tanmaysinghx/ts-api-engine-service-1606.git
git clone https://github.com/tanmaysinghx/ts-auth-service-1625.git
git clone https://github.com/tanmaysinghx/ts-notification-service-1689.git
git clone https://github.com/tanmaysinghx/ts-profile-engine-1676.git
git clone https://github.com/tanmaysinghx/ts-ticket-service-1674.git
cd ..

# --- FRONTEND ---
mkdir -p frontend
cd frontend
git clone https://github.com/tanmaysinghx/ts-umt-ui-1780.git
git clone https://github.com/tanmaysinghx/ts-pmt-ui-1725.git
cd ..