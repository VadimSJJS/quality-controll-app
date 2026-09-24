@echo off
echo Starting Quality Control App...
set DB_URL=jdbc:oracle:thin:@172.16.21.45:1521/plusora
set DB_USERNAME=prb
set DB_PASSWORD=rmn_ppb
set JWT_SECRET=your_strong_jwt_secret_here
set REMEMBER_ME_KEY=your_remember_me_secret_key_here
java -jar target\quality-control-app-0.0.1-SNAPSHOT.jar
pause