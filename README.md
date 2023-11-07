# AMov_TP_Reversi_Grupo35
Reversi app game

## Introduction
This project is a mobile implementation of the Reversi/Othello game, developed as part of the Mobile Architectures course. It aims to provide a versatile and engaging gaming experience for users.

## Architecture
- Developed in Kotlin
- Targeted for Android devices with API 22 or newer
- Utilizes the ViewModel architecture, separating game logic from the UI
- Key data classes: `CelulaTabuleiro`, `Tabuleiro`, `Jogador`, and `TrocaDados`
- Game state is managed by the `GameViewModel` class

## Features
- Authentication and user account management (login, registration, password recovery)
- Three gameplay modes (Mode 1, Mode 2, and Mode 3)
- Special moves (Bomba and Troca)
- Profile management with profile picture and username
- Online gameplay with socket communication
- Multi-language support (Portuguese and English)
- Design for both portrait and landscape orientations


## Functionality

### Login
- Users must log in to access the app
- Utilizes Firebase Authentication (email-based login)

### Registration
- Users can create an account with their name, email, and password
- Firebase Authentication is used for registration

### Forgot Password
- Users can recover their password by providing their email
- Utilizes Firebase Authentication for password recovery

### Rules
- Game rules can be accessed from within the game

### Mode 1
- Two players can play on the same device
- Allows toggling hints on and off
- Special moves include "Bomba" (bomb) and "Troca" (swap)
- One-time use for each special move per player

### Mode 2
- Two players can play on separate devices over the internet
- Communication between devices via sockets
- One device serves as the server
- Features the same rules and special moves as Mode 1

### Mode 3
- Modified version of the game for three players
- Three devices can play over the internet
- Adjusted rules for three players
- Special moves are available
- One device acts as the server

### Profile
- User profile information, including a profile picture, username, and email
- Users can change their profile picture using the device's camera
- Allows changing the username
- Displays the top five scores for each player

### Credits
- Provides information about the project authors

## Observations
- The game is not suitable for small screen layouts as the game board cells are not designed to resize.
- In Mode 2, the game cannot be continued if the connection with the opponent is lost.

## Authors
- [pmspedrosa](https://github.com/pmspedrosa)
- [APC15](https://github.com/APC15)https://github.com/APC15
- [C4CP10](https://github.com/C4CP10)https://github.com/C4CP10
