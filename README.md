# Menu Mate

Menu Mate is a digital solution for Restaurants, streamlining the ordering process by allowing customers to scan a QR code to access a digital menu and place orders. <br> This project was developed as part of my project-based learning journey in Android app development and serves as my first venture into using Kotlin. I am completing this project earlier than planned due to my upcoming board exams next month.

## Contents

1. [Features](#features)
2. [Flow chart and Screenshots](#screenshots)
3. [Animations Showcase](#animations-showcase)
4. [Future Enhancements](#future-enhancements)
5. [New Learnings](#new-learnings)
6. [Repository Contents](#repository-contents)
7. [Acknowledgments](#acknowledgments)

## Features

- **Customer Features**

  - Scan QR codes to access a digital menu.
  - Browse food items categorized by type.
  - Add items to the cart and place orders.
  - Save favorite dishes for quick access.
  - Apply special instructions and promo codes during checkout.
  - Search through food items

- **Restaurant Owner Features**

  - View orders in real-time through a dashboard.
  - Manage menu items, including adding and removing dishes.
  - Mark orders as ready once prepared.
  - Setup and customize restaurant details, such as name, description, and menu items.
 

- **Additional Features**

  - User authentication with Google and Twitter sign-in.
  - Logout functionality.


## Screenshots
![flow chart and scs](https://github.com/user-attachments/assets/33a23b75-bd67-42a9-8b6e-67d7286965c9)

## Animations Showcase

Below are some of the animations used in the app to enhance user experience:

- **Smooth Transitions**: Implemented seamless transitions between different screens.
- **Interactive Button Animations**: Added visual feedback for button clicks and interactions to improve usability.
---

**Scan Qr**

   https://github.com/user-attachments/assets/3bbac1a4-8afd-43d2-97d1-2e51ccd0cfa8

**Third party Scanner**

   https://github.com/user-attachments/assets/d1e37285-5418-4ef4-8aea-44e7770580bd

**Login Screen**

   https://github.com/user-attachments/assets/a51dc441-b250-463a-9b00-4b00cc52eda6

 **Home screen**

   https://github.com/user-attachments/assets/50413ea2-3248-422c-a7f2-82635076dadf


**Restaurant dashboard**

   https://github.com/user-attachments/assets/143b552f-3fc5-457d-b36a-ae467faf93e1

**Generate QR**

   https://github.com/user-attachments/assets/53229d4a-e816-48bb-bfe8-dd4ff381a24b

**Restaurant Setup**

   https://github.com/user-attachments/assets/3641dc5c-0f5f-4998-9128-ed517f4bf022

**Swipe Dismiss Cart Items**

   https://github.com/user-attachments/assets/2d74fa94-48ec-46c6-9414-cfce12324cd5

**Food Item Transition**

   https://github.com/user-attachments/assets/08094a09-c874-4079-a42f-67273b05eaea


## Future Enhancements

- Adding a payment gateway for seamless transactions.
- Implementing push notifications for order updates.
- Introducing analytics for restaurant owners to track popular dishes and sales trends.
- Offline functionality for order taking and menu viewing.

## New Learnings

While working on this project, I gained significant experience and knowledge in the following areas, including the concepts of MVVM (Model-View-ViewModel), Clean Architecture, and Dagger Hilt for dependency injection:

1. **Kotlin Basics and Android Development**\
   This was my first project in Kotlin, and I learned the syntax, structure, and core concepts of the language, including null safety, extensions, and higher-order functions.

2. **QR Code Integration**

   - Implemented QR code scanning functionality using third-party libraries to enhance the user experience.

3. **RecyclerView and Nested RecyclerViews**

   - Designed dynamic and nested RecyclerViews for displaying menu items and categories efficiently.

4. **HTTP Requests with OkHttp**

   - Used OkHttp for seamless data transfer between the Android app and a Node.js server for file uploads .

5. **State Management**

   - Managed app states efficiently using the MVVM (Model-View-ViewModel) architecture, which helped maintain a clear separation of concerns and made the codebase more modular, testable, scalable.

## Repository Contents

This repository includes:

- **Azure Local Emulator Server**: A local server setup for testing and simulating real-time database interactions.
- **Source Code**: The complete Kotlin-based Android application source code.
- **Signed APK**: A ready-to-install APK for testing the app without building from the source.

## Acknowledgments

- **Libraries Used**:

  - ZXing for QR code scanning.
  - Room for local sql db.
  - Lottie for json based animations.
  - Dagger Hilt for dependency injection.
  - Firebase Authentication and Realtime Database.
  - OkHttp for HTTP requests.
  - Material Design for UI components.
  
- **AI Part:**

   - V0 For design insperations
   - Chat Gpt And Claude for Debuging and Troubleshooting

- **Special Thanks** to the Android development community for the resources and tutorials that guided me through this learning journey.

