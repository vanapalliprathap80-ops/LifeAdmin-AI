# LifeAdmin API Specification

This document is the source of truth for all API contracts across the LifeAdmin application.

> **Status Notice**: All endpoints listed below are **PLANNED ENDPOINTS** for subsequent implementation phases (Phases 3 through 6).
> **Zero business logic or fake controllers** are implemented in Phase 1.

---

## Standard Response Envelope

All API endpoints return responses wrapped in a consistent structure:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

In case of error:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable description"
  },
  "timestamp": "2026-09-18T12:00:00Z"
}
```

---

## Document Management Endpoints (Planned: Phase 3 & 4)

### 1. Upload Document
- **Method**: `POST`
- **Path**: `/api/documents`
- **Content-Type**: `multipart/form-data`
- **Request Body**:
  - `file`: PDF binary (max 50MB)
- **Response**: `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "filename": "lease_agreement.pdf",
    "status": "UPLOADED",
    "uploadedAt": "2026-09-18T12:00:00Z"
  },
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

### 2. List Documents
- **Method**: `GET`
- **Path**: `/api/documents`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "filename": "lease_agreement.pdf",
      "status": "PROCESSED",
      "documentType": "LEASE_AGREEMENT",
      "uploadedAt": "2026-09-18T12:00:00Z"
    }
  ],
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

### 3. Get Document Details
- **Method**: `GET`
- **Path**: `/api/documents/{id}`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "filename": "lease_agreement.pdf",
    "status": "PROCESSED",
    "documentType": "LEASE_AGREEMENT",
    "metadata": {},
    "obligations": [],
    "actions": [],
    "uploadedAt": "2026-09-18T12:00:00Z"
  },
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

### 4. Get Document Status
- **Method**: `GET`
- **Path**: `/api/documents/{id}/status`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "EXTRACTING",
    "progressPercentage": 45,
    "errorMessage": null
  },
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

### 5. Reprocess Document
- **Method**: `POST`
- **Path**: `/api/documents/{id}/reprocess`
- **Response**: `202 Accepted`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "QUEUED"
  },
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

---

## Action Center Endpoints (Planned: Phase 6)

### 6. List Actions
- **Method**: `GET`
- **Path**: `/api/actions`
- **Query Params**:
  - `status`: (optional) `PENDING`, `COMPLETED`, `OVERDUE`
  - `priority`: (optional) `HIGH`, `MEDIUM`, `LOW`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "documentId": "uuid",
      "title": "Pay Monthly Rent",
      "description": "Submit rent payment of $1,500 before the 1st of each month",
      "dueDate": "2026-10-01",
      "status": "PENDING",
      "priority": "HIGH"
    }
  ],
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

### 7. Mark Action Completed
- **Method**: `PATCH`
- **Path**: `/api/actions/{id}/complete`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "COMPLETED",
    "completedAt": "2026-09-18T12:00:00Z"
  },
  "error": null,
  "timestamp": "2026-09-18T12:00:00Z"
}
```

---

## Infrastructure Endpoints (Available in Phase 1)

### Health Check
- **Method**: `GET`
- **Path**: `/actuator/health`
- **Response**: `200 OK`
```json
{
  "status": "UP"
}
```

### Swagger Documentation
- **Path**: `/swagger-ui/index.html` or `/swagger-ui.html`
- **Path**: `/v3/api-docs`
