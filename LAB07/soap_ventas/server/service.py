# server/service.py

from spyne import Application, rpc, ServiceBase, Unicode, Integer, Float
from spyne.protocol.soap import Soap11
from spyne.server.wsgi import WsgiApplication

class VentaService(ServiceBase):

    @rpc(Unicode, Integer, Float, _returns=Unicode)
    def registrar_venta(ctx, producto, cantidad, precio):
        total = cantidad * precio
        return f"Se registró la venta de {cantidad} unidad(es) de {producto} a ${precio:.2f} c/u. Total: ${total:.2f}"

application = Application(
    [VentaService],
    tns='urn:ventas.soap',
    in_protocol=Soap11(validator='lxml'),
    out_protocol=Soap11()
)

soap_app = WsgiApplication(application)

