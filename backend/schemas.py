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
    user_type: str          # intern / employee
    full_name: str
    user_id: int


class ChangePasswordRequest(BaseModel):
    old_password: str
    new_password: str


# ─── Position ─────────────────────────────────────────────────────────────────
class PositionResponse(BaseModel):
    id: int
    name: str
    is_manager: bool

    class Config:
        from_attributes = True


# ─── User (shared base) ───────────────────────────────────────────────────────
class UserBase(BaseModel):
    employee_code: str
    full_name: str
    role: Optional[str] = "user"
    user_type: Optional[str] = "intern"
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
    position: Optional[str] = None
    position_id: Optional[int] = None
    join_date: Optional[date] = None
    allowance: Optional[str] = "Không"
    employee_type: Optional[str] = "TTS Trung tâm"
    working_status: Optional[str] = "Working"
    employment_type: Optional[str] = "Fulltime"
    account_status: Optional[int] = 1

    # Employee-only
    direct_manager: Optional[str] = None
    computer_serial: Optional[str] = None
    employment_status: Optional[str] = "Thử việc"
    use_company_mac: Optional[str] = "Không"
    staff_category: Optional[str] = "NS trung tâm"
    seat_position: Optional[str] = None
    borrow_end_date: Optional[date] = None
    borrow_project: Optional[str] = None
    borrow_pm: Optional[str] = None
    borrow_center: Optional[str] = None


class UserCreate(UserBase):
    pass


class UserUpdate(BaseModel):
    full_name: Optional[str] = None
    role: Optional[str] = None
    user_type: Optional[str] = None
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
    position: Optional[str] = None
    position_id: Optional[int] = None
    join_date: Optional[date] = None
    allowance: Optional[str] = None
    employee_type: Optional[str] = None
    working_status: Optional[str] = None
    employment_type: Optional[str] = None
    account_status: Optional[int] = None
    direct_manager: Optional[str] = None
    computer_serial: Optional[str] = None
    employment_status: Optional[str] = None
    use_company_mac: Optional[str] = None
    staff_category: Optional[str] = None
    seat_position: Optional[str] = None
    borrow_end_date: Optional[date] = None
    borrow_project: Optional[str] = None
    borrow_pm: Optional[str] = None
    borrow_center: Optional[str] = None


class UserResponse(UserBase):
    id: int
    created_at: Optional[datetime] = None
    position_name: Optional[str] = None  # Tên vị trí (joined)

    class Config:
        from_attributes = True


# ─── Employee ─────────────────────────────────────────────────────────────────
class EmployeeCreate(BaseModel):
    """Schema tạo nhân viên mới – defaults phù hợp với employee"""
    employee_code: str
    full_name: str
    role: Optional[str] = "user"
    user_type: Optional[str] = "employee"
    gender: Optional[str] = None
    ethnicity: Optional[str] = None
    viettel_email: Optional[str] = None
    birthday: Optional[date] = None
    hometown: Optional[str] = None
    phone: Optional[str] = None
    cccd: Optional[str] = None
    bank_name: Optional[str] = "Viettel Money"
    bank_account: Optional[str] = None
    project: Optional[str] = None
    position: Optional[str] = None
    position_id: Optional[int] = None
    join_date: Optional[date] = None
    direct_manager: Optional[str] = None
    computer_serial: Optional[str] = None
    employment_status: Optional[str] = "Thử việc"
    use_company_mac: Optional[str] = "Không"
    staff_category: Optional[str] = "NS trung tâm"
    seat_position: Optional[str] = None
    borrow_end_date: Optional[date] = None
    borrow_project: Optional[str] = None
    borrow_pm: Optional[str] = None
    borrow_center: Optional[str] = None
    account_status: Optional[int] = 1


class EmployeeUpdate(BaseModel):
    full_name: Optional[str] = None
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
    position: Optional[str] = None
    position_id: Optional[int] = None
    join_date: Optional[date] = None
    direct_manager: Optional[str] = None
    computer_serial: Optional[str] = None
    employment_status: Optional[str] = None
    use_company_mac: Optional[str] = None
    staff_category: Optional[str] = None
    seat_position: Optional[str] = None
    borrow_end_date: Optional[date] = None
    borrow_project: Optional[str] = None
    borrow_pm: Optional[str] = None
    borrow_center: Optional[str] = None
    account_status: Optional[int] = None


class EmployeeResponse(BaseModel):
    id: int
    employee_code: str
    full_name: str
    role: str
    user_type: str
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
    position: Optional[str] = None
    position_id: Optional[int] = None
    position_name: Optional[str] = None
    join_date: Optional[date] = None
    direct_manager: Optional[str] = None
    computer_serial: Optional[str] = None
    employment_status: Optional[str] = None
    use_company_mac: Optional[str] = None
    staff_category: Optional[str] = None
    seat_position: Optional[str] = None
    borrow_end_date: Optional[date] = None
    borrow_project: Optional[str] = None
    borrow_pm: Optional[str] = None
    borrow_center: Optional[str] = None
    account_status: int
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class EmployeeImportResult(BaseModel):
    success: int
    skipped: int
    renamed: List[dict]  # [{"original": "nv_luongtd", "actual": "nv_luongtd1"}]
    message: str


class ManagerResponse(BaseModel):
    id: int
    full_name: str
    username: Optional[str] = None
    position_name: Optional[str] = None

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


# ─── Documents & AI ──────────────────────────────────────────────────────────
class DocumentResponse(BaseModel):
    id: int
    title: str
    filename: str
    status: str
    is_active: bool
    uploaded_by: Optional[str] = None
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True

class AIConfigUpdate(BaseModel):
    provider: Optional[str] = None
    api_key: Optional[str] = None
    chat_model: Optional[str] = None
    embedding_model: Optional[str] = None
    top_k: Optional[int] = None
    chunk_size: Optional[int] = None
    overlap: Optional[int] = None
    temperature: Optional[float] = None

class AIConfigResponse(BaseModel):
    id: int
    provider: str
    api_key: Optional[str] = None
    chat_model: str
    embedding_model: str
    top_k: int
    chunk_size: int
    overlap: int
    temperature: float

    class Config:
        from_attributes = True

class ChatRequest(BaseModel):
    message: str

class ChatSource(BaseModel):
    document_id: str
    title: str
    page: Optional[int] = None

class ChatResponse(BaseModel):
    answer: str
    sources: List[ChatSource]
