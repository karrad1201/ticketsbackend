alter table organization_applications
    add column if not exists contact_email   text         null,
    add column if not exists contact_phone   text         null,
    add column if not exists document_urls   jsonb        not null default '[]'::jsonb;
