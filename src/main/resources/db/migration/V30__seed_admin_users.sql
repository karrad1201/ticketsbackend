-- Seed admin users: 79272344839 и 79608991989, пароль "admin" (bcrypt cost=10)
insert into users (id, phone, full_name, role, email, avatar_url, interests)
values
  ('a0000000-0000-0000-0000-000000000001', '+79272344839', 'Admin 1', 'ADMIN', null, null, '[]'),
  ('a0000000-0000-0000-0000-000000000002', '+79608991989', 'Admin 2', 'ADMIN', null, null, '[]')
on conflict (phone) do update set role = 'ADMIN', full_name = excluded.full_name;

-- $2b$10$jHZKzWhNDax6/GTxz/Zct..0KF7R4sdgiVVIslv/9/ARq5iT5u7oa = bcrypt("admin")
insert into admin_credentials (user_id, password_hash)
select id, '$2b$10$jHZKzWhNDax6/GTxz/Zct..0KF7R4sdgiVVIslv/9/ARq5iT5u7oa'
from users where phone in ('+79272344839', '+79608991989')
on conflict (user_id) do nothing;
