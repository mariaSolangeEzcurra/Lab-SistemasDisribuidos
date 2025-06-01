# client/client.py

from zeep import Client

# Dirección del servicio
wsdl_url = 'http://localhost:8000/?wsdl'
cliente = Client(wsdl=wsdl_url)

# Llamar al método registrar_venta
respuesta = cliente.service.registrar_venta("Laptop", 2, 899.99)
print(respuesta)
