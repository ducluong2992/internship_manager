import os
import shutil
from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, BackgroundTasks
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas
from typing import List
from auth import get_current_user
import ai_service

router = APIRouter(prefix="/api/documents", tags=["documents"])

@router.post("/upload", response_model=schemas.DocumentResponse)
def upload_document(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")

    if not (file.filename.lower().endswith('.pdf') or 
            file.filename.lower().endswith('.docx') or 
            file.filename.lower().endswith('.txt')):
        raise HTTPException(status_code=400, detail="Only PDF, DOCX, TXT files are allowed")

    db_doc = models.Document(
        title=file.filename,
        filename=file.filename,
        uploaded_by=current_user.full_name,
        status="PROCESSING",
        is_active=True
    )
    db.add(db_doc)
    db.commit()
    db.refresh(db_doc)

    file_path = os.path.join(ai_service.UPLOAD_DIR, f"{db_doc.id}_{file.filename}")
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    def process_task(doc_id, path, fname, dbtitle):
        db_local = next(get_db())
        try:
            ai_service.process_document(doc_id, path, fname, dbtitle, db_local)
            doc = db_local.query(models.Document).get(doc_id)
            doc.status = "READY"
            db_local.commit()
        except Exception as e:
            print(f"Error processing doc {doc_id}: {e}")
            doc = db_local.query(models.Document).get(doc_id)
            if doc:
                doc.status = "ERROR"
                db_local.commit()
        finally:
            db_local.close()

    background_tasks.add_task(process_task, db_doc.id, file_path, file.filename, db_doc.title)

    return db_doc

@router.get("", response_model=List[schemas.DocumentResponse])
def get_documents(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")
    return db.query(models.Document).order_by(models.Document.created_at.desc()).all()

@router.delete("/{doc_id}")
def delete_document(doc_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")
    
    doc = db.query(models.Document).filter(models.Document.id == doc_id).first()
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    
    file_path = os.path.join(ai_service.UPLOAD_DIR, f"{doc.id}_{doc.filename}")
    if os.path.exists(file_path):
        os.remove(file_path)
    
    ai_service.delete_document_from_vector_store(str(doc.id))

    db.delete(doc)
    db.commit()
    return {"message": "Deleted successfully"}

@router.put("/{doc_id}/toggle", response_model=schemas.DocumentResponse)
def toggle_document(doc_id: int, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")
    
    doc = db.query(models.Document).filter(models.Document.id == doc_id).first()
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found")
    
    doc.is_active = not doc.is_active
    db.commit()
    db.refresh(doc)
    return doc
