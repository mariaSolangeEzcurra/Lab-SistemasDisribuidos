from flask import Flask, request, render_template_string
from zeep import Client

app = Flask(__name__)

wsdl_url = 'http://localhost:8000/?wsdl'
cliente = Client(wsdl=wsdl_url)

inventario = {
    "Laptop": 899.99,
    "Celular": 499.90,
    "Auriculares": 59.99,
    "Teclado": 39.50,
    "Mouse": 25.00,
    "Tablet": 349.90
}

ventas = []

HTML_FORM = """
<!doctype html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Registrar Venta</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet"/>
  <style>
  body {
    background: #fafafa;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    padding: 50px 15px;
    color: #222;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .container {
    max-width: 720px;
    background: #fff;
    padding: 40px 50px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.07);
    transition: transform 0.2s ease;
  }
  .container:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 18px rgba(0,0,0,0.1);
  }
  h2 {
    font-weight: 700;
    margin-bottom: 1rem;
    color: #222;
  }
  hr {
    border-color: #eee;
    margin-bottom: 30px;
  }
  label {
    font-weight: 600;
    font-size: 1.1rem;
    color: #444;
  }
  select.form-select, input.form-control {
    font-size: 1.05rem;
    padding: 12px 14px;
    border-radius: 8px;
    border: 1.5px solid #ccc;
    transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
    background: #fafafa;
    color: #333;
  }
  select.form-select:focus, input.form-control:focus {
    border-color: #6c8ed9;
    box-shadow: 0 0 8px rgba(108, 142, 217, 0.3);
    outline: none;
    background: #fff;
  }
  input[readonly] {
    background-color: #e9ecef !important;
    color: #495057;
    cursor: not-allowed;
  }
  .btn-primary {
    background-color: #6c8ed9;
    border: none;
    font-weight: 700;
    padding: 14px;
    font-size: 1.2rem;
    border-radius: 10px;
    width: 100%;
    margin-top: 15px;
    box-shadow: 0 4px 12px rgba(108,142,217,0.4);
    transition: background-color 0.3s ease;
    color: white;
  }
  .btn-primary:hover {
    background-color: #5879c1;
    box-shadow: 0 6px 18px rgba(88,121,193,0.6);
  }
  .btn-secondary {
    width: 100%;
    padding: 14px;
    font-size: 1.15rem;
    border-radius: 10px;
    margin-top: 25px;
    color: #6c8ed9;
    background-color: transparent;
    border: 2px solid #6c8ed9;
    font-weight: 600;
    transition: background-color 0.3s ease, color 0.3s ease;
  }
  .btn-secondary:hover {
    background-color: #6c8ed9;
    color: white;
  }
  .alert {
    margin-top: 25px;
    border-radius: 8px;
    font-size: 1.1rem;
    padding: 15px 20px;
    font-weight: 600;
  }
  .alert-success {
    background-color: #d4e2fc;
    color: #234a8a;
    border: 1px solid #6c8ed9;
  }
  .alert-danger {
    background-color: #f8d7da;
    color: #842029;
    border: 1px solid #f5c2c7;
  }
</style>

</head>
<body>
  <div class="container shadow-sm">
    
    <h2>Registrar Venta</h2>
    <hr>
    <form method="post" novalidate onsubmit="return validarFormulario()">
      <div class="mb-4">
        <label for="producto" class="form-label">Producto</label>
        <select id="producto" name="producto" class="form-select" required onchange="actualizarPrecio()">
          <option value="" selected disabled>Seleccione un producto</option>
          {% for nombre in inventario %}
            <option value="{{ nombre }}">{{ nombre }}</option>
          {% endfor %}
        </select>
        <div class="invalid-feedback">Por favor, seleccione un producto.</div>
      </div>

      <div class="mb-4">
        <label for="cantidad" class="form-label">Cantidad</label>
        <input type="number" id="cantidad" name="cantidad" class="form-control" min="1" required placeholder="Ingrese cantidad" />
        <div class="invalid-feedback">Por favor, ingrese una cantidad válida (mayor a 0).</div>
      </div>

      <div class="mb-4">
        <label for="precio" class="form-label">Precio Unitario (USD)</label>
        <input type="text" id="precio" name="precio" class="form-control" readonly required />
      </div>

      <button type="submit" class="btn btn-primary">Registrar Venta</button>
    </form>

    <form method="get" action="/historial">
      <button type="submit" class="btn btn-secondary">Ver Historial de Ventas</button>
    </form>

    {% if respuesta %}
      <div class="alert alert-success" role="alert">{{ respuesta }}</div>
    {% endif %}
    {% if error %}
      <div class="alert alert-danger" role="alert">{{ error }}</div>
    {% endif %}
  </div>

<script>
  const precios = {{ inventario | tojson }};
  const productoSelect = document.getElementById('producto');
  const precioInput = document.getElementById('precio');
  const cantidadInput = document.getElementById('cantidad');

  function actualizarPrecio() {
    const producto = productoSelect.value;
    if (producto && precios[producto] !== undefined) {
      precioInput.value = precios[producto].toFixed(2);
    } else {
      precioInput.value = '';
    }
  }

  function validarFormulario() {
    let valido = true;

    // Validar producto
    if (!productoSelect.value) {
      productoSelect.classList.add('is-invalid');
      valido = false;
    } else {
      productoSelect.classList.remove('is-invalid');
    }

    // Validar cantidad
    const cantidadVal = parseInt(cantidadInput.value, 10);
    if (isNaN(cantidadVal) || cantidadVal < 1) {
      cantidadInput.classList.add('is-invalid');
      valido = false;
    } else {
      cantidadInput.classList.remove('is-invalid');
    }

    // Validar precio (debe existir)
    if (!precioInput.value) {
      valido = false;
      // no se marca porque es readonly, pero evita enviar sin precio
    }

    return valido;
  }
  
  // Para que si vuelves a la página y hay valor seleccionado, se muestre el precio
  document.addEventListener('DOMContentLoaded', () => {
    actualizarPrecio();
  });
</script>
</body>
</html>
"""
HTML_HISTORIAL = """
<!doctype html>
<html lang="es">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Historial de Ventas</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet"/>
  <style>
  body {
    background: #fafafa;
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    padding: 50px 15px;
    color: #222;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .container {
    max-width: 720px;
    background: #fff;
    padding: 40px 50px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.07);
    transition: transform 0.2s ease;
  }
  .container:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 18px rgba(0,0,0,0.1);
  }
  h2 {
    font-weight: 700;
    margin-bottom: 1rem;
    color: #222;
  }
  hr {
    border-color: #eee;
    margin-bottom: 30px;
  }
  table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 25px;
  }
  thead {
    background-color: #6c8ed9;
    color: white;
  }
  th, td {
    padding: 12px 15px;
    border: 1px solid #ddd;
    text-align: center;
  }
  tbody tr:nth-child(even) {
    background-color: #f9f9f9;
  }
  .btn-primary {
    background-color: #6c8ed9;
    border: none;
    font-weight: 700;
    padding: 14px;
    font-size: 1.2rem;
    border-radius: 10px;
    width: 100%;
    box-shadow: 0 4px 12px rgba(108,142,217,0.4);
    transition: background-color 0.3s ease;
    color: white;
    cursor: pointer;
  }
  .btn-primary:hover {
    background-color: #5879c1;
    box-shadow: 0 6px 18px rgba(88,121,193,0.6);
  }
  .empty-msg {
    font-style: italic;
    color: #999;
    font-weight: 600;
    text-align: center;
    margin-bottom: 20px;
  }
  </style>
</head>
<body>
  <div class="container shadow-sm">
    <h2>Historial de Ventas</h2>
    <hr>
    {% if ventas %}
    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Producto</th>
          <th>Cantidad</th>
          <th>Precio Unitario (USD)</th>
          <th>Total (USD)</th>
        </tr>
      </thead>
      <tbody>
        {% for v in ventas %}
        <tr>
          <td>{{ loop.index }}</td>
          <td>{{ v['producto'] }}</td>
          <td>{{ v['cantidad'] }}</td>
          <td>${{ "%.2f"|format(v['precio']) }}</td>
          <td>${{ "%.2f"|format(v['total']) }}</td>
        </tr>
        {% endfor %}
      </tbody>
    </table>
    {% else %}
      <p class="empty-msg">No se han registrado ventas aún.</p>
    {% endif %}
    <form action="/" method="get">
      <button type="submit" class="btn btn-primary">Registrar Venta</button>
    </form>
  </div>
</body>
</html>
"""

@app.route('/', methods=['GET', 'POST'])
def index():
    respuesta = None
    error = None
    if request.method == 'POST':
        try:
            producto = request.form.get('producto')
            cantidad = int(request.form.get('cantidad', 0))
            precio = float(request.form.get('precio', 0))

            if producto not in inventario:
                raise ValueError("Producto no válido.")
            if cantidad < 1 or precio < 0:
                raise ValueError("Cantidad debe ser mayor a 0 y precio no puede ser negativo.")

            total = cantidad * precio
            respuesta = cliente.service.registrar_venta(producto, cantidad, precio)

            ventas.append({
                'producto': producto,
                'cantidad': cantidad,
                'precio': precio,
                'total': total
            })
        except Exception as e:
            error = str(e)

    return render_template_string(HTML_FORM, respuesta=respuesta, error=error, inventario=inventario)

@app.route('/historial')
def historial():
    return render_template_string(HTML_HISTORIAL, ventas=ventas)

if __name__ == '__main__':
    app.run(debug=True)
