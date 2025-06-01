# server/run_server.py

from werkzeug.serving import run_simple
from service import soap_app

if __name__ == '__main__':
    print("Servicio SOAP corriendo en http://localhost:8000/?wsdl")
    run_simple('127.0.0.1', 8000, soap_app)
