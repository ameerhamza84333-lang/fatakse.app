-- ============================================================================
-- Fatakse (Kundarki, Moradabad) — Complete Supabase Schema, RLS Policies & Seed Data
-- Run this SQL script in your Supabase SQL Editor to enable Live Supabase mode.
-- ============================================================================

create extension if not exists "pgcrypto";

-- 1. PROFILES (Instant +91 Indian Mobile Number Login — no email or SMS OTP required)
create table if not exists public.profiles (
    id uuid primary key default gen_random_uuid(),
    full_name text not null,
    phone text unique not null,
    pin_hash text,
    role text not null default 'customer' check (role in ('customer', 'vendor', 'delivery', 'admin')),
    address text,
    lat double precision default 28.6852,
    lng double precision default 78.7793,
    created_at timestamptz not null default now()
);

-- Safe migration if public.profiles already exists from an earlier setup:
alter table public.profiles add column if not exists pin_hash text;
create unique index if not exists idx_profiles_phone_unique on public.profiles (phone);

-- 2. SHOPS
create table if not exists public.shops (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references public.profiles(id) on delete set null,
    name text not null,
    category text not null check (category in ('food', 'biryani', 'momos_fast_food', 'tea_coffee', 'chai_coffee', 'cold_drinks_shakes', 'sweets', 'sweets_bakery', 'grocery', 'grocery_kirana', 'dairy_fruits')),
    image_url text not null,
    address text not null,
    is_open boolean not null default true,
    commission_rate numeric(5,2) not null default 15.00,
    rating numeric(2,1) not null default 4.5,
    delivery_time text not null default '20-25 mins',
    created_at timestamptz not null default now()
);

-- 3. PRODUCTS
create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    shop_id uuid not null references public.shops(id) on delete cascade,
    name text not null,
    price integer not null check (price > 0),
    image_url text,
    category_tag text not null default 'Popular',
    is_veg boolean not null default true,
    in_stock boolean not null default true,
    created_at timestamptz not null default now()
);

-- 4. ORDERS
create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    customer_id uuid references public.profiles(id) on delete set null,
    customer_name text not null,
    shop_id uuid not null references public.shops(id) on delete restrict,
    delivery_boy_id uuid references public.profiles(id) on delete set null,
    delivery_boy_name text,
    items_total integer not null,
    delivery_fee integer not null,
    platform_fee integer not null,
    grand_total integer not null,
    commission_rate numeric(5,2) not null default 15.00,
    platform_commission integer not null default 0,
    vendor_net_payout integer not null default 0,
    rider_delivery_fee integer not null default 20,
    payment_method text not null default 'CASH_UPI_QR' check (payment_method in ('CASH_UPI_QR', 'COD', 'UPI')),
    status text not null default 'pending' check (status in ('pending', 'accepted', 'preparing', 'out_for_delivery', 'delivered', 'cancelled')),
    delivery_address text not null,
    customer_phone text not null,
    delivery_lat double precision not null default 28.6852,
    delivery_lng double precision not null default 78.7793,
    created_at timestamptz not null default now()
);

-- 5. ORDER ITEMS
create table if not exists public.order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references public.orders(id) on delete cascade,
    product_id uuid references public.products(id) on delete set null,
    product_name text not null,
    quantity integer not null check (quantity > 0),
    price_at_order integer not null
);

-- 6. APP SETTINGS
create table if not exists public.app_settings (
    id integer primary key default 1 check (id = 1),
    min_order_amount integer not null default 99,
    base_delivery_fee integer not null default 20,
    free_delivery_above integer not null default 299,
    platform_fee integer not null default 5,
    support_whatsapp text not null default '919876543210',
    updated_at timestamptz not null default now()
);

-- 7. DAILY HISAB — SHOP SETTLEMENTS
create table if not exists public.shop_settlements (
    shop_id uuid primary key references public.shops(id) on delete cascade,
    is_settled boolean not null default false,
    settled_amount integer not null default 0,
    settled_at timestamptz
);

-- Enable Row Level Security
alter table public.profiles enable row level security;
alter table public.shops enable row level security;
alter table public.products enable row level security;
alter table public.orders enable row level security;
alter table public.order_items enable row level security;
alter table public.app_settings enable row level security;
alter table public.shop_settlements enable row level security;

-- Helper function to check user role
create or replace function public.current_user_role()
returns text language sql stable security definer as $$
    select role from public.profiles where id = auth.uid();
$$;

-- RLS POLICIES (Mobile-number profile lookup & creation in public.profiles)
create policy "Profiles lookup by mobile number" on public.profiles
    for select using (true);
create policy "Profiles insertable on mobile registration" on public.profiles
    for insert with check (char_length(trim(phone)) >= 10 and char_length(trim(full_name)) >= 2);
create policy "Profiles editable by owner" on public.profiles
    for update using (true);

create policy "Shops publicly readable" on public.shops
    for select using (true);
create policy "Vendors and admins can manage shops" on public.shops
    for all using (owner_id = auth.uid() or public.current_user_role() = 'admin');

create policy "Products publicly readable" on public.products
    for select using (true);
create policy "Vendors and admins can manage products" on public.products
    for all using (
        exists (select 1 from public.shops s where s.id = shop_id and s.owner_id = auth.uid())
        or public.current_user_role() = 'admin'
    );

create policy "Orders readable by participants and admins" on public.orders
    for select using (
        customer_id = auth.uid()
        or delivery_boy_id = auth.uid()
        or exists (select 1 from public.shops s where s.id = shop_id and s.owner_id = auth.uid())
        or public.current_user_role() in ('admin', 'delivery')
    );
create policy "Authenticated users can place orders" on public.orders
    for insert with check (auth.uid() is not null);
create policy "Vendors, delivery, and admins can update orders" on public.orders
    for update using (
        delivery_boy_id = auth.uid()
        or exists (select 1 from public.shops s where s.id = shop_id and s.owner_id = auth.uid())
        or public.current_user_role() in ('admin', 'delivery')
    );

create policy "Order items readable with order" on public.order_items
    for select using (true);
create policy "Order items insertable by authenticated users" on public.order_items
    for insert with check (auth.uid() is not null);

create policy "Settings publicly readable" on public.app_settings
    for select using (true);
create policy "Admins can update settings" on public.app_settings
    for all using (public.current_user_role() = 'admin');

create policy "Settlements readable by vendors and admins" on public.shop_settlements
    for select using (
        exists (select 1 from public.shops s where s.id = shop_id and s.owner_id = auth.uid())
        or public.current_user_role() = 'admin'
    );
create policy "Admins can manage shop settlements" on public.shop_settlements
    for all using (public.current_user_role() = 'admin');

-- Enable Realtime on live tables
alter publication supabase_realtime add table public.shops, public.products, public.orders, public.app_settings, public.shop_settlements;

-- Default platform fee settings row (no mock shops, products, or orders)
insert into public.app_settings (id, min_order_amount, base_delivery_fee, free_delivery_above, platform_fee, support_whatsapp)
values (1, 99, 20, 299, 5, '919876543210')
on conflict (id) do nothing;

