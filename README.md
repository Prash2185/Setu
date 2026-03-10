# ☁️ Zoho SETU: Cloud Storage & Automated Billing Engine

An Enterprise-level Minimum Viable Product (MVP) for a Cloud Storage platform that allows users to upload files, calculates storage usage, and dynamically generates automated tax invoices. 

## 🚀 Features
* **File I/O Engine:** Securely uploads and stores files directly to the local hard drive.
* **Usage Metering:** Automatically calculates file size in Megabytes (MB) upon upload.
* **Persistent Database:** Uses H2 Database in File Mode (`.mv.db`) to ensure data is permanently saved even after server restarts.
* **Automated Billing:** Generates a dynamic HTML Invoice calculating the total storage used multiplied by a base rate (₹15/MB).
* **Zero Configuration:** Runs locally without needing complex MySQL installations.

## 🛠️ Tech Stack
* **Backend:** Java 17, Spring Boot 3.x
* **Database:** H2 Database (Persistent File Mode), Spring Data JPA, Hibernate
* **Architecture:** MVC (Model-View-Controller), REST API
* **Build Tool:** Maven

## 🔌 API Endpoints
| HTTP Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Returns the HTML UI for the File Upload Form. |
| `POST` | `/upload` | Receives `MultipartFile`, saves to drive, calculates size, and logs metadata to DB. |
| `GET` | `/invoice` | Fetches all records from DB, calculates total usage, and renders the Final Tax Invoice. |

## ⚙️ How to Run Locally
1. Clone this repository.
2. Ensure you have Java 17 installed.
3. Open terminal in the project directory and run: `.\mvnw spring-boot:run`
4. Access the application at `http://localhost:8081`

*Note: The application creates a local folder at `D:/CloudStorage/` to save the physical files.*