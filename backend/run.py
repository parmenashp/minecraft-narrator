import uvicorn

if __name__ == "__main__":
    uvicorn.run("src.main:app", port=5000, reload=True, workers=2)
