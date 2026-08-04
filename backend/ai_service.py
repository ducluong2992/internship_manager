import os
import fitz  # PyMuPDF
import docx
import chromadb
from google import genai
from google.genai import types as genai_types
from langchain_text_splitters import RecursiveCharacterTextSplitter
from sqlalchemy.orm import Session
from models import AIConfig, Document

CHROMA_PATH = os.path.join(os.path.dirname(__file__), "chroma_db")
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")

os.makedirs(CHROMA_PATH, exist_ok=True)
os.makedirs(UPLOAD_DIR, exist_ok=True)

chroma_client = chromadb.PersistentClient(path=CHROMA_PATH)
collection = chroma_client.get_or_create_collection(name="hrm_documents")

def get_ai_config(db: Session):
    config = db.query(AIConfig).first()
    if not config:
        config = AIConfig()
        db.add(config)
        db.commit()
        db.refresh(config)
    return config

def extract_text(file_path: str, filename: str):
    text_data = []
    if filename.lower().endswith('.pdf'):
        doc = fitz.open(file_path)
        for page_num in range(len(doc)):
            page = doc.load_page(page_num)
            text = page.get_text()
            if text.strip():
                text_data.append({"text": text, "page": page_num + 1})
        doc.close()
    elif filename.lower().endswith('.docx'):
        doc = docx.Document(file_path)
        text = "\n".join([para.text for para in doc.paragraphs])
        if text.strip():
            text_data.append({"text": text, "page": 1})
    elif filename.lower().endswith('.txt'):
        with open(file_path, "r", encoding="utf-8") as f:
            text = f.read()
            if text.strip():
                text_data.append({"text": text, "page": 1})
    return text_data

def process_document(document_id: str, file_path: str, filename: str, title: str, db: Session):
    config = get_ai_config(db)
    if not config.api_key:
        raise Exception("API Key is not configured")

    embed_client = genai.Client(api_key=config.api_key, http_options={"api_version": "v1"})

    text_data = extract_text(file_path, filename)

    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=config.chunk_size,
        chunk_overlap=config.overlap,
        separators=["\n\n", "\n", " ", ""]
    )

    docs_to_add = []
    metadatas_to_add = []
    ids_to_add = []

    chunk_index = 0
    for data in text_data:
        chunks = text_splitter.split_text(data["text"])
        for chunk in chunks:
            clean_chunk = "\n".join([line.strip() for line in chunk.split("\n") if line.strip()])
            if not clean_chunk:
                continue

            docs_to_add.append(clean_chunk)
            metadatas_to_add.append({
                "document_id": str(document_id),
                "title": title,
                "page": data["page"],
                "chunk_index": chunk_index
            })
            ids_to_add.append(f"{document_id}_{chunk_index}")
            chunk_index += 1

    if not docs_to_add:
        return

    def get_embedding(text):
        result = embed_client.models.embed_content(
            model=config.embedding_model,
            contents=text,
            config=genai_types.EmbedContentConfig(task_type="RETRIEVAL_DOCUMENT")
        )
        return result.embeddings[0].values

    embeddings = [get_embedding(doc) for doc in docs_to_add]

    collection.add(
        documents=docs_to_add,
        embeddings=embeddings,
        metadatas=metadatas_to_add,
        ids=ids_to_add
    )

def delete_document_from_vector_store(document_id: str):
    collection.delete(
        where={"document_id": str(document_id)}
    )

def ask_assistant(question: str, db: Session):
    config = get_ai_config(db)
    if not config.api_key:
        return {"answer": "Admin chưa cấu hình API Key cho trợ lý AI.", "sources": []}

    embed_client = genai.Client(api_key=config.api_key, http_options={"api_version": "v1"})
    chat_client = genai.Client(api_key=config.api_key)

    try:
        query_embedding_res = embed_client.models.embed_content(
            model=config.embedding_model,
            contents=question,
            config=genai_types.EmbedContentConfig(task_type="RETRIEVAL_QUERY")
        )
        query_embedding = query_embedding_res.embeddings[0].values
    except Exception as e:
        return {"answer": f"Lỗi khi tạo embedding: {str(e)}", "sources": []}

    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=config.top_k
    )

    if not results["documents"] or not results["documents"][0]:
        return {"answer": "Tôi không tìm thấy thông tin.", "sources": []}

    docs = results["documents"][0]
    metas = results["metadatas"][0]

    active_docs = db.query(Document).filter(Document.is_active == True).all()
    active_doc_ids = [str(d.id) for d in active_docs]

    filtered_docs = []
    filtered_metas = []
    for d, m in zip(docs, metas):
        if str(m.get("document_id")) in active_doc_ids:
            filtered_docs.append(d)
            filtered_metas.append(m)

    if not filtered_docs:
        return {"answer": "Tôi không tìm thấy thông tin trong các tài liệu hiện có.", "sources": []}

    context = ""
    for i, (doc, meta) in enumerate(zip(filtered_docs, filtered_metas)):
        context += f"--- Chunk {i+1} ---\n{doc}\n\n"

    prompt = f"Bạn là trợ lý AI của Trung tâm.\nChỉ trả lời dựa trên tài liệu dưới đây.\nNếu không tìm thấy hãy trả lời: \"Tôi không tìm thấy thông tin.\"\n\n-------------------\n{context}-------------------\nCâu hỏi: {question}\n"

    CHAT_FALLBACKS = [config.chat_model, "gemini-flash-latest", "gemini-flash-lite-latest", "gemini-3.1-flash-lite"]
    answer = "Lỗi: Không thể kết nối đến mô hình AI."
    for model_name in CHAT_FALLBACKS:
        try:
            response = chat_client.models.generate_content(
                model=model_name,
                contents=prompt,
                config=genai_types.GenerateContentConfig(temperature=config.temperature)
            )
            answer = response.text
            break
        except Exception as e:
            err_str = str(e)
            if "429" in err_str or "RESOURCE_EXHAUSTED" in err_str:
                import time; time.sleep(2)
                continue
            elif "404" in err_str:
                continue
            else:
                answer = f"Lỗi từ mô hình AI: {err_str}"
                break

    sources = []
    seen = set()
    for m in filtered_metas:
        key = (m.get("document_id"), m.get("page"))
        if key not in seen:
            seen.add(key)
            sources.append({
                "document_id": str(m.get("document_id")),
                "title": m.get("title"),
                "page": m.get("page")
            })

    return {"answer": answer, "sources": sources}
