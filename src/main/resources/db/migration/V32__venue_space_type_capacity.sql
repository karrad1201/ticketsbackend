alter table venue_spaces add column if not exists type varchar(20) not null default 'ADMISSION';
alter table venue_spaces add column if not exists capacity int not null default 0;
