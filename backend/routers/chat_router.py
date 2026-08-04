from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas
from auth import get_current_user
import ai_service

router = APIRouter(prefix="/api/chat", tags=["chat"])

@router.post("", response_model=schemas.ChatResponse)
def chat_with_assistant(
    request: schemas.ChatRequest,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    result = ai_service.ask_assistant(request.message, db)
    return result
