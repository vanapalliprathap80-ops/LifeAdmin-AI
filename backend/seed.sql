-- Get the default user (assuming one was created by SecurityConfig/schema)
DO $$
DECLARE
    default_user_id UUID;
    doc1_id UUID := gen_random_uuid();
    doc2_id UUID := gen_random_uuid();
BEGIN
    SELECT id INTO default_user_id FROM users LIMIT 1;
    
    IF default_user_id IS NULL THEN
        -- If no user exists, create one
        default_user_id := gen_random_uuid();
        INSERT INTO users (id, first_name, last_name, email, password_hash, created_at, updated_at)
        VALUES (default_user_id, 'Demo', 'User', 'demo@lifeadmin.com', 'password', NOW(), NOW());
    END IF;

    -- Insert Documents
    INSERT INTO documents (id, user_id, original_filename, storage_path, content_type, file_size, status, created_at, updated_at, ai_summary)
    VALUES 
    (doc1_id, default_user_id, 'Car_Insurance_Policy.pdf', 'fake/path1.pdf', 'application/pdf', 204800, 'COMPLETED', NOW() - INTERVAL '2 days', NOW(), 'Comprehensive auto insurance policy covering liability, collision, and comprehensive damage for the 2022 Toyota Camry.'),
    (doc2_id, default_user_id, 'Lease_Agreement_2026.pdf', 'fake/path2.pdf', 'application/pdf', 1024000, 'COMPLETED', NOW() - INTERVAL '5 days', NOW(), 'Residential lease agreement for Apartment 4B. 12-month term starting next month. Rent is $2,000/month.');

    -- Insert Key Dates
    INSERT INTO key_dates (id, document_id, date_value, date_type, description, created_at, updated_at)
    VALUES 
    (gen_random_uuid(), doc1_id, CURRENT_DATE + INTERVAL '6 months', 'EXPIRY', 'Policy Expiration Date', NOW(), NOW()),
    (gen_random_uuid(), doc1_id, CURRENT_DATE + INTERVAL '15 days', 'PAYMENT', 'Next Premium Payment Due', NOW(), NOW());

    -- Insert Obligations
    INSERT INTO obligations (id, document_id, user_id, obligation_type, title, description, due_date, status, created_at, updated_at)
    VALUES 
    (gen_random_uuid(), doc1_id, default_user_id, 'PAYMENT', 'Pay Auto Insurance Premium', 'Pay $150.00 for the monthly premium.', CURRENT_DATE + INTERVAL '15 days', 'PENDING', NOW(), NOW()),
    (gen_random_uuid(), doc2_id, default_user_id, 'PAYMENT', 'Pay Security Deposit', 'Pay $2,000 security deposit before move-in.', CURRENT_DATE + INTERVAL '10 days', 'PENDING', NOW(), NOW());

    -- Insert Actions
    INSERT INTO actions (id, document_id, user_id, title, description, priority, is_completed, created_at, updated_at)
    VALUES 
    (gen_random_uuid(), doc1_id, default_user_id, 'Review coverage limits', 'Check if the current liability limits are sufficient for the new year.', 'MEDIUM', false, NOW(), NOW());

    -- Insert User Services
    INSERT INTO user_services (id, user_id, service_name, service_type, provider, renewal_date, cost, currency, status, auto_renew, created_at, updated_at)
    VALUES 
    (gen_random_uuid(), default_user_id, 'Netflix Subscription', 'ENTERTAINMENT', 'Netflix', CURRENT_DATE + INTERVAL '5 days', 15.99, 'USD', 'ACTIVE', true, NOW(), NOW());

    -- Insert Notifications
    INSERT INTO notifications (id, user_id, title, message, category, is_read, created_at)
    VALUES 
    (gen_random_uuid(), default_user_id, 'New Obligation Detected', 'Found a new payment obligation: Pay Auto Insurance Premium', 'OBLIGATION_DETECTED', false, NOW() - INTERVAL '2 hours'),
    (gen_random_uuid(), default_user_id, 'Service Renewing Soon', 'Your Netflix Subscription will auto-renew in 5 days.', 'DEADLINE_APPROACHING', true, NOW() - INTERVAL '1 day');
END $$;
