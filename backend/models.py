from sqlalchemy import Column, Integer, String, Date, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    employee_code = Column(String, unique=True, nullable=False)
    full_name = Column(String, nullable=False)
    role = Column(String, default="intern")  # admin / intern
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
    allowance = Column(Integer, default=0)
    employee_type = Column(String, default="Intern")  # Intern / Borrowed
    working_status = Column(String, default="Working")  # Working / Resigned
    employment_type = Column(String, default="Fulltime")  # Fulltime / Parttime
    account_status = Column(Integer, default=1)  # 1=active, 0=locked
    created_at = Column(DateTime, default=func.now())

    schedules = relationship("Schedule", back_populates="user")
    account = relationship("Account", back_populates="user", uselist=False, cascade="all, delete-orphan")


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
