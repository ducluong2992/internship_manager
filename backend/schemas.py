from pydantic import BaseModel, EmailStr
from typing import Optional, List
from datetime import date, datetime


# ─── Auth ───────────────────────────────────────────────────────────────────
class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str
    role: str
    full_name: str
    user_id: int


class ChangePasswordRequest(BaseModel):
    old_password: str
    new_password: str


# ─── User ────────────────────────────────────────────────────────────────────
class UserBase(BaseModel):
    employee_code: str
    full_name: str
    role: Optional[str] = "intern"
    gender: Optional[str] = None
    ethnicity: Optional[str] = None
    viettel_email: Optional[str] = None
    birthday: Optional[date] = None
    hometown: Optional[str] = None
    phone: Optional[str] = None
    cccd: Optional[str] = None
    bank_name: Optional[str] = None
    bank_account: Optional[str] = None
    project: Optional[str] = None
    allowance: Optional[int] = 0
    employee_type: Optional[str] = "Intern"
    working_status: Optional[str] = "Working"
    employment_type: Optional[str] = "Fulltime"
    account_status: Optional[int] = 1


class UserCreate(UserBase):
    pass


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    role: Optional[str] = None
    gender: Optional[str] = None
    ethnicity: Optional[str] = None
    viettel_email: Optional[str] = None
    birthday: Optional[date] = None
    hometown: Optional[str] = None
    phone: Optional[str] = None
    cccd: Optional[str] = None
    bank_name: Optional[str] = None
    bank_account: Optional[str] = None
    project: Optional[str] = None
    allowance: Optional[int] = None
    employee_type: Optional[str] = None
    working_status: Optional[str] = None
    employment_type: Optional[str] = None
    account_status: Optional[int] = None


class UserResponse(UserBase):
    id: int
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


# ─── Schedule Period ──────────────────────────────────────────────────────────
class PeriodCreate(BaseModel):
    month: int
    year: int
    open_date: Optional[datetime] = None
    close_date: Optional[datetime] = None
    status: Optional[str] = "closed"


class PeriodUpdate(BaseModel):
    open_date: Optional[datetime] = None
    close_date: Optional[datetime] = None
    status: Optional[str] = None


class PeriodResponse(BaseModel):
    id: int
    month: int
    year: int
    open_date: Optional[datetime] = None
    close_date: Optional[datetime] = None
    status: str

    class Config:
        from_attributes = True


# ─── Schedule ─────────────────────────────────────────────────────────────────
class ScheduleEntry(BaseModel):
    work_day: date
    shift: Optional[str] = None  # S / C / SC / None


class ScheduleSubmit(BaseModel):
    period_id: int
    entries: List[ScheduleEntry]


class ScheduleResponse(BaseModel):
    id: int
    period_id: int
    user_id: int
    work_day: date
    shift: Optional[str] = None
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class AdminScheduleRow(BaseModel):
    user_id: int
    employee_code: str
    full_name: str
    schedules: List[ScheduleResponse]

    class Config:
        from_attributes = True


# ─── Account ──────────────────────────────────────────────────────────────────
class AccountResponse(BaseModel):
    id: int
    user_id: int
    username: str
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class AdminAccountRow(BaseModel):
    user_id: int
    employee_code: str
    full_name: str
    username: Optional[str] = None
    account_status: int

    class Config:
        from_attributes = True
