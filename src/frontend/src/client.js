import fetch from 'unfetch';

const checkStatus = response => {
    if (response.ok) {
        return response;
    }
    // convert non-2xx HTTP responses into errors:
    const error = new Error(response.statusText);
    error.response = response;
    return Promise.reject(error);
}

export const getAllStudents = () =>
    fetch("api/v1/students")
        .then(checkStatus);

export const addNewStudent = student =>
    fetch("api/v1/students", {
            headers: {
                'Content-Type': 'application/json'
            },
            method: 'POST',
            body: JSON.stringify(student)
        }
    ).then(checkStatus)

export const deleteStudent = studentId =>
    fetch(`api/v1/students/${studentId}`, {
        method: 'DELETE'
    }).then(checkStatus);

// HRMS API Endpoints
export const getAllWorkers = () =>
    fetch("api/workers")
        .then(checkStatus);

export const createWorker = worker =>
    fetch("api/workers", {
        headers: { 'Content-Type': 'application/json' },
        method: 'POST',
        body: JSON.stringify(worker)
    }).then(checkStatus);

export const getAllSites = () =>
    fetch("api/sites")
        .then(checkStatus);

export const createSite = site =>
    fetch("api/sites", {
        headers: { 'Content-Type': 'application/json' },
        method: 'POST',
        body: JSON.stringify(site)
    }).then(checkStatus);

export const clockIn = request =>
    fetch("api/attendance/clock-in", {
        headers: { 'Content-Type': 'application/json' },
        method: 'POST',
        body: JSON.stringify(request)
    }).then(checkStatus);

export const clockOut = request =>
    fetch("api/attendance/clock-out", {
        headers: { 'Content-Type': 'application/json' },
        method: 'POST',
        body: JSON.stringify(request)
    }).then(checkStatus);

export const getActiveWorkers = () =>
    fetch("api/attendance/active")
        .then(checkStatus);

export const getOvertimeSummary = (workerId, month) =>
    fetch(`api/overtime/summary/${workerId}?month=${month}`)
        .then(checkStatus);

export const settleOvertime = (workerId, month) =>
    fetch(`api/overtime/settle/${workerId}?month=${month}`, {
        method: 'POST'
    }).then(checkStatus);
