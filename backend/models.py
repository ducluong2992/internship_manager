from sqlalchemy import Column, Integer, String, Date, DateTime, ForeignKey, Text, Boolean, Float
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base


class Position(Base):
    """Bảng vị trí công việc"""
    __tablename__ = "positions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String, unique=True, nullable=False)
    is_manager = Column(Boolean, default=False)  # True = có thể làm quản lý trực tiếp

    users = relationship("User", back_populates="position_rel")


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    employee_code = Column(String, unique=True, nullable=False)
    full_name = Column(String, nullable=False)
    role = Column(String, default="user")           # admin / user
    user_type = Column(String, default="intern")    # intern / employee
    gender = Column(String, nullable=True)
    ethnicity = Column(String, nullable=True)
    viettel_email = Column(String, nullable=True)
    birthday = Column(Date, nullable=True)
    hometown = Column(String, nullable=True)
    phone = Column(String, nullable=True)
    cccd = Column(String, nullable=True)
    bank_name = Column(String, nullable=True)
    bank_account = Column(String, nullable=True)
    project = Column(String, nullable=True)
    position = Column(String, nullable=True)        # Text field (legacy/intern)
    position_id = Column(Integer, ForeignKey("positions.id"), nullable=True)  # FK cho employee
    join_date = Column(Date, nullable=True)
    allowance = Column(String, default="Không")
    employee_type = Column(String, default="TTS Trung tâm")  # TTS Trung tâm / Đi mượn (intern)
    working_status = Column(String, default="Working")       # Working / Resigned
    employment_type = Column(String, default="Fulltime")     # Fulltime / Parttime

    # ─── Employee-only fields ───────────────────────────────
    direct_manager = Column(String, nullable=True)           # username của quản lý trực tiếp
    computer_serial = Column(String, nullable=True)          # Seri máy tính
    employment_status = Column(String, default="Thử việc")  # Thử việc / Chính thức
    use_company_mac = Column(String, default="Không")        # Có / Không
    staff_category = Column(String, default="NS trung tâm") # NS trung tâm / Onsite / Cho mượn
    seat_position = Column(String, nullable=True)            # Vị trí ngồi
    borrow_end_date = Column(Date, nullable=True)            # Thời hạn mượn
    borrow_project = Column(String, nullable=True)           # Dự án mượn
    borrow_pm = Column(String, nullable=True)                # Username PM dự án mượn
    borrow_center = Column(String, nullable=True)            # Trung tâm cho mượn
    # ────────────────────────────────────────────────────────

    account_status = Column(Integer, default=1)  # 1=active, 0=locked
    created_at = Column(DateTime, default=func.now())

    schedules = relationship("Schedule", back_populates="user")
    account = relationship("Account", back_populates="user", uselist=False, cascade="all, delete-orphan")
    position_rel = relationship("Position", back_populates="users")


class Account(Base):
    __tablename__ = "accounts"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, unique=True)
    username = Column(String, unique=True, nullable=False)
    password = Column(String, nullable=False)
    created_at = Column(DateTime, default=func.now())

    user = relationship("User", back_populates="account")


class SchedulePeriod(Base):
    __tablename__ = "schedule_period"

    id = Column(Integer, primary_key=True, autoincrement=True)
    month = Column(Integer, nullable=False)
    year = Column(Integer, nullable=False)
    open_date = Column(DateTime, nullable=True)
    close_date = Column(DateTime, nullable=True)
    status = Column(String, default="closed")  # open / closed

    schedules = relationship("Schedule", back_populates="period")


class Schedule(Base):
    __tablename__ = "schedules"

    id = Column(Integer, primary_key=True, autoincrement=True)
    period_id = Column(Integer, ForeignKey("schedule_period.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    work_day = Column(Date, nullable=False)
    shift = Column(String, nullable=True)  # S / C / SC / None
    created_at = Column(DateTime, default=func.now())

    period = relationship("SchedulePeriod", back_populates="schedules")
    user = relationship("User", back_populates="schedules")


class Document(Base):
    __tablename__ = "documents"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String, nullable=False)
    filename = Column(String, nullable=False)
    status = Column(String, default="PROCESSING")  # PROCESSING / READY / ERROR
    is_active = Column(Boolean, default=True)
    uploaded_by = Column(String, nullable=True)
    created_at = Column(DateTime, default=func.now())


class AIConfig(Base):
    __tablename__ = "ai_config"

    id = Column(Integer, primary_key=True, autoincrement=True)
    provider = Column(String, default="Google Gemini")
    api_key = Column(String, nullable=True)
    chat_model = Column(String, default="gemini-2.5-flash")
    embedding_model = Column(String, default="text-embedding-004")
    top_k = Column(Integer, default=5)
    chunk_size = Column(Integer, default=1000)
    overlap = Column(Integer, default=150)
    temperature = Column(Float, default=0.2)
