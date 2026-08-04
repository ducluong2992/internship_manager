import sqlite3

db_path = 'internship.db'
conn = sqlite3.connect(db_path)
cursor = conn.cursor()

# Check existing columns
cursor.execute('PRAGMA table_info(users)')
existing_cols = [row[1] for row in cursor.fetchall()]
print('Existing cols:', existing_cols)

# New columns to add: (name, SQL definition)
new_columns = [
    ('user_type',         'VARCHAR'),
    ('position_id',       'INTEGER'),
    ('direct_manager',    'VARCHAR'),
    ('computer_serial',   'VARCHAR'),
    ('employment_status', 'VARCHAR'),
    ('use_company_mac',   'VARCHAR'),
    ('staff_category',    'VARCHAR'),
    ('seat_position',     'VARCHAR'),
    ('borrow_end_date',   'DATE'),
    ('borrow_project',    'VARCHAR'),
    ('borrow_pm',         'VARCHAR'),
    ('borrow_center',     'VARCHAR'),
]

for col_name, col_def in new_columns:
    if col_name not in existing_cols:
        sql = f'ALTER TABLE users ADD COLUMN {col_name} {col_def}'
        cursor.execute(sql)
        print(f'  Added: {col_name}')
    else:
        print(f'  Exists: {col_name}')

# Set default user_type for existing users
cursor.execute("UPDATE users SET user_type = 'intern' WHERE user_type IS NULL AND role = 'intern'")
print(f'Set user_type=intern for {cursor.rowcount} records')

# Migrate old role=intern to role=user
cursor.execute("UPDATE users SET role = 'user', user_type = 'intern' WHERE role = 'intern'")
print(f'Migrated {cursor.rowcount} intern -> user records')

conn.commit()
print('Creating positions table if not exists...')

cursor.execute('''
CREATE TABLE IF NOT EXISTS positions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR UNIQUE NOT NULL,
    is_manager BOOLEAN DEFAULT 0
)
''')

positions = [
    ('Tro ly du an', 0),
    ('PM', 1),
    ('DU Lead', 1),
    ('GDTT', 1),
    ('PGDTT', 1),
    ('Dev', 0),
    ('Dev Lead', 1),
    ('Dev Mobile', 0),
    ('DevOps', 0),
    ('Tester', 0),
    ('Test Lead', 1),
    ('BA', 0),
    ('BA Lead', 1),
    ('QA', 0),
    ('DA', 0),
    ('AI', 0),
]

for name, is_mgr in positions:
    cursor.execute('INSERT OR IGNORE INTO positions (name, is_manager) VALUES (?, ?)', (name, is_mgr))

# Re-insert with correct Vietnamese names
positions_vn = [
    ('Trợ lý dự án', 0),
    ('PM', 1),
    ('DU Lead', 1),
    ('GDTT', 1),
    ('PGDTT', 1),
    ('Dev', 0),
    ('Dev Lead', 1),
    ('Dev Mobile', 0),
    ('DevOps', 0),
    ('Tester', 0),
    ('Test Lead', 1),
    ('BA', 0),
    ('BA Lead', 1),
    ('QA', 0),
    ('DA', 0),
    ('AI', 0),
]

# Delete ASCII versions and insert Vietnamese
cursor.execute('DELETE FROM positions')
for name, is_mgr in positions_vn:
    cursor.execute('INSERT OR IGNORE INTO positions (name, is_manager) VALUES (?, ?)', (name, is_mgr))

cursor.execute('SELECT COUNT(*) FROM positions')
print(f'Positions count: {cursor.fetchone()[0]}')

conn.commit()
conn.close()
print('Migration complete!')
