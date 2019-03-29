// manually created test data. No real health data was used
// Temp Basal Treatments
db.treatments.insert([
    {
        "created_at": "2019-01-01T12:00:00Z",
        "timestamp": "2019-01-01T12:00:00Z",
        "eventType": "Temp Basal",
        "duration": 30,
        "rate": 0.4,
        "absolute": 0.4
    },
    {
        "created_at": "2019-01-01T12:30:00Z",
        "timestamp": "2019-01-01T12:30:00Z",
        "eventType": "Temp Basal",
        "duration": 0,
        "rate": 0,
        "absolute": 0
    },
    {
        "created_at": "2019-01-01T13:00:00Z",
        "timestamp": "2019-01-01T13:00:00Z",
        "eventType": "Temp Basal",
        "duration": 15,
        "rate": 0.8,
        "absolute": 0.8
    },
    {
        "created_at": "2019-01-01T13:30:00Z",
        "timestamp": "2019-01-01T13:30:00Z",
        "eventType": "Temp Basal",
        "duration": 20,
        "rate": 1.35,
        "absolute": 1.35
    }
]);
// Meal Treatments
db.treatments.insert([
    {
        "created_at": "2019-01-01T12:00:00Z",
        "timestamp": "2019-01-01T12:00:00Z",
        "eventType": "Meal Bolus",
        "carbs": 30,
        "absorptionTime": 180
    },
    {
        "created_at": "2019-01-01T12:30:00Z",
        "timestamp": "2019-01-01T12:30:00Z",
        "eventType": "Meal Bolus",
        "carbs": 10,
        "absorptionTime": 180
    },
    {
        "created_at": "2019-01-01T13:00:00Z",
        "timestamp": "2019-01-01T13:00:00Z",
        "eventType": "Meal Bolus",
        "carbs": 20,
        "absorptionTime": 180
    }
]);
// Bolus Treatments
db.treatments.insert([
    {
        "created_at": "2019-01-01T12:00:00Z",
        "timestamp": "2019-01-01T12:00:00Z",
        "eventType": "Correction Bolus",
        "programmed": 1,
        "insulin": 1
    },
    {
        "created_at": "2019-01-01T12:30:00Z",
        "timestamp": "2019-01-01T12:30:00Z",
        "eventType": "Correction Bolus",
        "programmed": 2,
        "insulin": 2
    },
    {
        "created_at": "2019-01-01T13:00:00Z",
        "timestamp": "2019-01-01T13:00:00Z",
        "eventType": "Correction Bolus",
        "programmed": 3,
        "insulin": 3
    }
]);
