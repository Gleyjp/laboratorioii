package org.cibertec.edu.pe.laboratorioii.controller;

import org.cibertec.edu.pe.laboratorioii.model.Detalle;
import org.cibertec.edu.pe.laboratorioii.model.Producto;
import org.cibertec.edu.pe.laboratorioii.model.Venta;
import org.cibertec.edu.pe.laboratorioii.repository.IDetalleRepository;
import org.cibertec.edu.pe.laboratorioii.repository.IProductoRepository;
import org.cibertec.edu.pe.laboratorioii.repository.IVentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
@SessionAttributes({"carrito", "subtotal", "envio", "descuento", "total", "mensaje"})
public class ProductoController {
	@Autowired
	private IProductoRepository productoRepository;
	@Autowired
	private IVentaRepository ventaRepository;
	@Autowired
	private IDetalleRepository detalleRepository;
	
	@GetMapping("/index")
	public String listado(Model model) {
		List<Producto> lista = new ArrayList<>();
		lista = productoRepository.findAll();
		model.addAttribute("productos", lista);
		return "index";
	}

	@GetMapping("/agregar/{idProducto}")
	public String agregar(Model model, @PathVariable(name = "idProducto", required = true) int idProducto) {
		// Codigo para agregar un producto
		Producto producto = productoRepository.getReferenceById(idProducto);
		int cantidad = 1;
		Detalle detalleCarrito = null;

		List<Detalle> carrito = (ArrayList) model.getAttribute("carrito");
		for (Detalle detalle : carrito) {
			if(detalle.getProducto().getIdProducto()==idProducto){
				detalleCarrito = detalle;
				detalleCarrito.setCantidad(detalle.getCantidad() + 1);
				detalleCarrito.setSubtotal(detalle.getProducto().getPrecio() * detalleCarrito.getCantidad());
				break;
			}
		}

		if(detalleCarrito == null){
			detalleCarrito = new Detalle();
			detalleCarrito.setProducto(producto);
			detalleCarrito.setCantidad(cantidad);
			detalleCarrito.setSubtotal(cantidad * producto.getPrecio());
		}

		detalleRepository.save(detalleCarrito);

		List detalle = detalleRepository.findAll();
		model.addAttribute("carrito", detalle);

		return "redirect:/index";
	}
	
	@GetMapping("/carrito")
	public String carrito(Model model) {
		List<Detalle> detalle = new ArrayList<>();
		detalle = detalleRepository.findAll();
		model.addAttribute("carrito", detalle);

		calculoDetalleCompra(model);

		return "carrito";
	}
	
	@GetMapping("/pagar")
	public String pagar(Model model) {
	    // Codigo para pagar
		double total = (double) model.getAttribute("total");

		Venta venta = new Venta();
		venta.setFechaRegistro(new Date());
		venta.setMontoTotal(total);

		ventaRepository.save(venta);

		// Actualizar el stock del producto despues de la venta
		List<Detalle> detalleCarrito = (ArrayList) model.getAttribute("carrito");
		for (Detalle detalle : detalleCarrito) {
			Producto producto = productoRepository.findById(detalle.getProducto().getIdProducto()).get();
			int nuevoStock = producto.getStock() - detalle.getCantidad();
			producto.setStock(nuevoStock);

			productoRepository.save(producto);
		}

		//Eliminar los productos vendidos del carrito
		for (Detalle detalle : detalleCarrito) {
			detalleRepository.delete(detalle);
		}

		model.addAttribute("mensaje", "Gracias, su compra se realizo correctamente!");
	    return "mensaje";
	}

	@GetMapping("/eliminar/{idDetalle}")
	public String pagar(Model model, @PathVariable(name = "idDetalle", required = true) int idDetalle) {
		// Codigo para eliminar
		detalleRepository.deleteById(idDetalle);
		return "redirect:/carrito";
	}

	@PostMapping("/actualizarCarrito")
	public String actualizarCarrito(Model model) {
	    // Codigo para actualizar el carrito
		calculoDetalleCompra(model);

		return "carrito";
	}

	private void calculoDetalleCompra(Model model) {
		List<Detalle> detalleCarrito = (ArrayList) model.getAttribute("carrito");

		if(detalleCarrito.size()>0) {
			double sumaSubtotal = 0;
			int contadoDetalle = 0;
			double envio = 0;
			double descuento = 0;

			for (Detalle detalle : detalleCarrito) {
				contadoDetalle++;
				sumaSubtotal += detalle.getSubtotal();
			}

			if (contadoDetalle <= 2) {
				envio = 10;
			} else if (contadoDetalle > 2 && contadoDetalle <= 5) {
				envio = 8;
				descuento = 0.03;
			} else {
				envio = 0;
				descuento = 0.10;
			}

			model.addAttribute("envio", envio);
			model.addAttribute("descuento", descuento);
			model.addAttribute("subtotal", sumaSubtotal);
			model.addAttribute("total", sumaSubtotal + envio - descuento);
		}else{
			model.addAttribute("envio", 0.0);
			model.addAttribute("descuento", 0.0);
			model.addAttribute("subtotal", 0.0);
			model.addAttribute("total", 0.0);
		}
	}
	
	// Inicializacion de variable de la sesion
	@ModelAttribute("carrito")
	public List<Detalle> getCarrito() {
		return new ArrayList<Detalle>();
	}
	
	@ModelAttribute("subtotal")
	public double getSubTotal() {
		return 0.0;
	}

	@ModelAttribute("envio")
	public double getPrecioEnvio() {
		return 0.0;
	}

	@ModelAttribute("descuento")
	public double getDescuento() {
		return 0.0;
	}

	@ModelAttribute("total")
	public double getTotal() {
		return 0.0;
	}

	@ModelAttribute("mensaje")
	public String getMensaje() {
		return "";
	}
}
