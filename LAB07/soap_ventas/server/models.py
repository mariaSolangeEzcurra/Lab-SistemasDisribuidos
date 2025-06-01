# server/models.py

from spyne import ComplexModel, Unicode, Integer, Float

class Producto(ComplexModel):
    nombre = Unicode
    cantidad = Integer
    precio = Float
