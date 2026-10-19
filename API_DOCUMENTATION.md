# Job Management APIs Documentation

This document provides comprehensive API documentation for the job management system with Postman curl commands for testing.

## Base URL
```
http://localhost:8080
```

## Authentication
Most APIs require Bearer token authentication. Include the token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

---

## 1. Get All Jobs with Filters and Pagination

### Endpoint
```
GET /api/jobs
```

### Parameters
- `searchTerm` (optional): Search in job title, description, or requirements
- `jobType` (optional): Filter by job type (FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP, PROJECT_BASED)
- `experienceLevel` (optional): Filter by experience level (ENTRY_LEVEL, MID_LEVEL, SENIOR_LEVEL, EXPERT_LEVEL)
- `status` (optional): Filter by job status (DRAFT, ACTIVE, PAUSED, CLOSED, CANCELLED)
- `minPay` (optional): Minimum pay range
- `maxPay` (optional): Maximum pay range
- `location` (optional): Filter by location
- `isRemote` (optional): Filter remote jobs (true/false)
- `isUrgent` (optional): Filter urgent jobs (true/false)
- `isFeatured` (optional): Filter featured jobs (true/false)
- `skills` (optional): Comma-separated list of required skills
- `page` (default: 0): Page number (0-based)
- `size` (default: 10): Page size
- `sortBy` (default: "publishedAt"): Sort field
- `sortDir` (default: "desc"): Sort direction (asc/desc)

### Postman Curl Commands

#### Basic Get All Jobs
```bash
curl -X GET "http://localhost:8080/api/jobs" \
  -H "Content-Type: application/json"
```

#### Get Jobs with Filters
```bash
curl -X GET "http://localhost:8080/api/jobs?searchTerm=actor&jobType=FULL_TIME&location=Mumbai&page=0&size=10" \
  -H "Content-Type: application/json"
```

#### Get Remote Jobs Only
```bash
curl -X GET "http://localhost:8080/api/jobs?isRemote=true&page=0&size=20" \
  -H "Content-Type: application/json"
```

#### Get Jobs by Pay Range
```bash
curl -X GET "http://localhost:8080/api/jobs?minPay=50000&maxPay=100000&page=0&size=10" \
  -H "Content-Type: application/json"
```

#### Get Jobs by Skills
```bash
curl -X GET "http://localhost:8080/api/jobs?skills=acting,dancing,singing&page=0&size=10" \
  -H "Content-Type: application/json"
```

---

## 2. Get Job by ID

### Endpoint
```
GET /api/jobs/{id}
```

### Postman Curl Command
```bash
curl -X GET "http://localhost:8080/api/jobs/1" \
  -H "Content-Type: application/json"
```

---

## 3. Apply for Job

### Endpoint
```
POST /api/applications
```

### Request Body
```json
{
  "jobId": 1,
  "coverLetter": "I am very interested in this role...",
  "expectedSalary": 75000,
  "availabilityDate": "2024-01-15",
  "portfolioUrl": "https://example.com/portfolio",
  "resumeUrl": "https://example.com/resume.pdf",
  "demoReelUrl": "https://example.com/demo.mp4"
}
```

### Postman Curl Command
```bash
curl -X POST "http://localhost:8080/api/applications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "jobId": 1,
    "coverLetter": "I am very interested in this role and believe I would be a great fit.",
    "expectedSalary": 75000,
    "availabilityDate": "2024-01-15",
    "portfolioUrl": "https://example.com/portfolio",
    "resumeUrl": "https://example.com/resume.pdf",
    "demoReelUrl": "https://example.com/demo.mp4"
  }'
```

---

## 4. Get User Applications with Filters

### Endpoint
```
GET /api/applications/my-applications
```

### Parameters
- `page` (default: 0): Page number
- `size` (default: 10): Page size
- `status` (optional): Filter by application status (APPLIED, REVIEWED, SHORTLISTED, INTERVIEW_SCHEDULED, HIRED, REJECTED, WITHDRAWN)
- `jobTitle` (optional): Search by job title
- `companyName` (optional): Search by company name
- `sortBy` (default: "appliedAt"): Sort field
- `sortDir` (default: "desc"): Sort direction

### Postman Curl Commands

#### Get All My Applications
```bash
curl -X GET "http://localhost:8080/api/applications/my-applications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### Get Applications by Status
```bash
curl -X GET "http://localhost:8080/api/applications/my-applications?status=APPLIED&page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### Search Applications by Job Title
```bash
curl -X GET "http://localhost:8080/api/applications/my-applications?jobTitle=actor&page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### Search Applications by Company
```bash
curl -X GET "http://localhost:8080/api/applications/my-applications?companyName=ABC%20Studios&page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

---

## 5. Bookmark Job

### Endpoint
```
POST /api/bookmarks/{jobId}
```

### Request Body (Optional)
```json
{
  "notes": "Interesting role for future reference"
}
```

### Postman Curl Commands

#### Bookmark Job (Simple)
```bash
curl -X POST "http://localhost:8080/api/bookmarks/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### Bookmark Job with Notes
```bash
curl -X POST "http://localhost:8080/api/bookmarks/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "notes": "Interesting role for future reference"
  }'
```

---

## 6. Get Bookmarked Jobs with Filters

### Endpoint
```
GET /api/bookmarks
```

### Parameters
- `page` (default: 0): Page number
- `size` (default: 10): Page size
- `activeOnly` (default: true): Show only active jobs
- `jobTitle` (optional): Search by job title
- `companyName` (optional): Search by company name
- `jobType` (optional): Filter by job type
- `location` (optional): Filter by location
- `sortBy` (default: "bookmarkedAt"): Sort field
- `sortDir` (default: "desc"): Sort direction

### Postman Curl Commands

#### Get All My Bookmarks
```bash
curl -X GET "http://localhost:8080/api/bookmarks" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### Get Bookmarks with Filters
```bash
curl -X GET "http://localhost:8080/api/bookmarks?jobTitle=actor&location=Mumbai&page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

#### Get Bookmarks by Job Type
```bash
curl -X GET "http://localhost:8080/api/bookmarks?jobType=FULL_TIME&page=0&size=10" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

---

## 7. Additional Job APIs

### Get Remote Jobs
```bash
curl -X GET "http://localhost:8080/api/jobs/remote" \
  -H "Content-Type: application/json"
```

### Get Featured Jobs
```bash
curl -X GET "http://localhost:8080/api/jobs/featured" \
  -H "Content-Type: application/json"
```

### Get Urgent Jobs
```bash
curl -X GET "http://localhost:8080/api/jobs/urgent" \
  -H "Content-Type: application/json"
```

### Search Jobs
```bash
curl -X GET "http://localhost:8080/api/jobs/search?q=actor&page=0&size=10" \
  -H "Content-Type: application/json"
```

### Get Jobs by Location
```bash
curl -X GET "http://localhost:8080/api/jobs/location/Mumbai" \
  -H "Content-Type: application/json"
```

### Get Jobs by Type
```bash
curl -X GET "http://localhost:8080/api/jobs/type/FULL_TIME" \
  -H "Content-Type: application/json"
```

### Get Jobs by Skill
```bash
curl -X GET "http://localhost:8080/api/jobs/skill/acting" \
  -H "Content-Type: application/json"
```

---

## 8. Bookmark Management APIs

### Remove Bookmark
```bash
curl -X DELETE "http://localhost:8080/api/bookmarks/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Remove Bookmark by Job ID
```bash
curl -X DELETE "http://localhost:8080/api/bookmarks/job/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Check if Job is Bookmarked
```bash
curl -X GET "http://localhost:8080/api/bookmarks/check/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Get Bookmark Statistics
```bash
curl -X GET "http://localhost:8080/api/bookmarks/stats" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

---

## 9. Application Management APIs

### Get Application by ID
```bash
curl -X GET "http://localhost:8080/api/applications/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Check Application Status for Job
```bash
curl -X GET "http://localhost:8080/api/applications/check/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

### Withdraw Application
```bash
curl -X POST "http://localhost:8080/api/applications/1/withdraw" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>"
```

---

## Response Format

All APIs return responses in the following format:

### Success Response
```json
{
  "success": true,
  "data": [...],
  "totalElements": 100,
  "totalPages": 10,
  "currentPage": 0,
  "size": 10
}
```

### Error Response
```json
{
  "success": false,
  "message": "Error message",
  "error": "Detailed error information"
}
```

---

## Authentication

To get a JWT token for testing, use the authentication endpoint:

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password"
  }'
```

Replace `<your-jwt-token>` in the curl commands with the actual token received from the login response.

---

## Artist Profile Photos (Front, Left, Right)

Each image is uploaded independently. Request a presigned URL, PUT the image bytes to
`presignedUrl` using the same `Content-Type` supplied in the request, then save the returned
`fileUrl` with `PUT /api/artists/profile`. The photo URLs are stored in separate columns;
omitted fields are left unchanged.

### Generate a presigned URL

`POST /api/upload/presigned-url` (authenticated):

```json
{
  "fileName": "front.jpg",
  "fileType": "image/jpeg",
  "fileSize": 2400000,
  "uploadType": "PROFILE_PHOTO",
  "photoType": "FRONT"
}
```

`photoType` must be `FRONT`, `LEFT`, or `RIGHT`. Allowed image types are JPEG/JPG, PNG,
and WebP; each image must be no larger than 5 MB. Upload the bytes directly to S3 using
the returned `presignedUrl` and the same `Content-Type`.

### Save one or more uploaded image URLs

`PUT /api/artists/profile` (authenticated), with any subset of:

```json
{
  "rightProfilePhoto": "https://icastar-uploads.s3.ap-south-1.amazonaws.com/profiles/123/right_1234567890.jpg"
}
```

The artist profile responses include `frontProfilePhoto`, `leftProfilePhoto`, and
`rightProfilePhoto`, including `null` for images that have not been uploaded.

---

## Testing Notes

1. **Base URL**: Update the base URL if your application runs on a different port
2. **Authentication**: Ensure you have a valid JWT token for protected endpoints
3. **Pagination**: Use `page` and `size` parameters for pagination
4. **Filtering**: Combine multiple filter parameters for refined searches
5. **Sorting**: Use `sortBy` and `sortDir` parameters for custom sorting

All APIs support comprehensive filtering, pagination, and search capabilities as requested.
