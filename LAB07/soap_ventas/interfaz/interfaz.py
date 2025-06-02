from flask import Flask, request, render_template_string, redirect, url_for, session
from zeep import Client

app = Flask(__name__)
app.secret_key = 'clave_secreta_segura'  # Necesario para manejar sesiones

wsdl_url = 'http://localhost:8000/?wsdl'
cliente = Client(wsdl=wsdl_url)

usuarios = {
    "admin": "1234",
    "usuario": "clave"
}

inventario = {
    "Laptop": 899.99,
    "Celular": 499.90,
    "Auriculares": 59.99,
    "Teclado": 39.50,
    "Mouse": 25.00,
    "Tablet": 349.90
}

ventas = []

HTML_LOGIN = """
<!doctype html>
<html lang="es">
<head><meta charset="utf-8"><title>Login</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="d-flex justify-content-center align-items-center vh-100 bg-light">
  <div class="card p-4 shadow" style="width: 100%; max-width: 400px">
    <h2 class="mb-4">Iniciar sesión</h2>
    {% if error %}<div class="alert alert-danger">{{ error }}</div>{% endif %}
    <form method="post">
      <div class="mb-3">
        <label class="form-label">Usuario</label>
        <input type="text" name="usuario" class="form-control" required>
      </div>
      <div class="mb-3">
        <label class="form-label">Contraseña</label>
        <input type="password" name="clave" class="form-control" required>
      </div>
      <button type="submit" class="btn btn-primary w-100">Ingresar</button>
    </form>
  </div>
</body>
</html>
"""

# Aquí está tu HTML_FORM original con el botón de cerrar sesión agregado arriba
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
    flex-direction: column;
  }
  .container {
    max-width: 720px;
    background: #fff;
    padding: 40px 50px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.07);
    transition: transform 0.2s ease;
    width: 100%;
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
  .btn-logout {
    display: block;
    width: 100%;               /* ocupa todo el ancho */
    margin: 30px auto; /* margen arriba y abajo de 30px, centrado horizontalmente */
    background-color: #dc3545;
    border: none;
    color: white;
    font-weight: 700;
    padding: 10px 18px;
    border-radius: 10px;
    cursor: pointer;
    transition: background-color 0.3s ease;
  }

  .btn-logout:hover {
    background-color: #b02a37;
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

    <form method="get" action="/logout">
      <button type="submit" class="btn-logout">Cerrar sesión</button>
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
    }

    return valido;
  }
  
  document.addEventListener('DOMContentLoaded', () => {
    actualizarPrecio();
  });
</script>
</body>
</html>
"""

# Aquí está tu HTML_HISTORIAL original con el botón de cerrar sesión agregado arriba
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
    flex-direction: column;
  }
  .container {
    max-width: 720px;
    background: #fff;
    padding: 40px 50px;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.07);
    width: 100%;
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
    margin-top: 20px;
  }
  th, td {
    border: 1px solid #ddd;
    padding: 12px 15px;
    text-align: left;
  }
  th {
    background-color: #6c8ed9;
    color: white;
    font-weight: 700;
  }
  tr:nth-child(even) {
    background-color: #f9f9f9;
  }
  .btn-secondary, .btn-logout {
    width: 100%;
    padding: 14px;
    font-size: 1.15rem;
    border-radius: 10px;
    margin-top: 25px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.3s ease;
  }
  .btn-secondary {
    color: #6c8ed9;
    background-color: transparent;
    border: 2px solid #6c8ed9;
  }
  .btn-secondary:hover {
    background-color: #6c8ed9;
    color: white;
  }
  .btn-logout {
  display: block;
  margin: 30px auto; /* margen arriba y abajo de 30px, centrado horizontalmente */
  background-color: #dc3545;
  border: none;
  color: white;
  font-weight: 700;
  padding: 10px 18px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

  .btn-logout:hover {
    background-color: #b02a37;
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
            <th>Producto</th>
            <th>Cantidad</th>
            <th>Precio Unitario (USD)</th>
          </tr>
        </thead>
        <tbody>
          {% for venta in ventas %}
            <tr>
              <td>{{ venta['producto'] }}</td>
              <td>{{ venta['cantidad'] }}</td>
              <td>{{ venta['precio'] }}</td>
            </tr>
          {% endfor %}
        </tbody>
      </table>
    {% else %}
      <p>No hay ventas registradas aún.</p>
    {% endif %}
    <form method="get" action="/">
      <button type="submit" class="btn btn-secondary">Registrar Nueva Venta</button>
    </form>

    <form method="get" action="/logout">
      <button type="submit" class="btn-logout">Cerrar sesión</button>
    </form>
  </div>
</body>
</html>
"""

@app.route('/login', methods=['GET', 'POST'])
def login():
    error = None
    if request.method == 'POST':
        usuario = request.form.get('usuario')
        clave = request.form.get('clave')
        if usuario in usuarios and usuarios[usuario] == clave:
            session['usuario'] = usuario
            return redirect(url_for('index'))
        else:
            error = "Usuario o clave incorrectos"
    return render_template_string(HTML_LOGIN, error=error)

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('login'))

@app.route('/', methods=['GET', 'POST'])
def index():
    if 'usuario' not in session:
        return redirect(url_for('login'))

    respuesta = None
    error = None
    if request.method == 'POST':
        producto = request.form.get('producto')
        cantidad = request.form.get('cantidad')
        precio = request.form.get('precio')

        if not producto or not cantidad or not precio:
            error = "Complete todos los campos."
        else:
            try:
                cantidad_int = int(cantidad)
                precio_float = float(precio)
                # Usamos el servicio SOAP para registrar la venta
                resultado = cliente.service.registrar_venta(producto, cantidad_int, precio_float)
                if resultado:
                    ventas.append({"producto": producto, "cantidad": cantidad_int, "precio": precio_float})
                    respuesta = "Venta registrada exitosamente."
                else:
                    error = "Error al registrar la venta."
            except Exception as e:
                error = f"Error en datos: {str(e)}"

    return render_template_string(HTML_FORM, inventario=inventario, respuesta=respuesta, error=error)

@app.route('/historial')
def historial():
    if 'usuario' not in session:
        return redirect(url_for('login'))

    return render_template_string(HTML_HISTORIAL, ventas=ventas)

if __name__ == '__main__':
    app.run(debug=True)
