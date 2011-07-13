
function loadPage(options){ 
  var b = new OpenLayers.Bounds(options.bounds[0], options.bounds[1], options.bounds[2], options.bounds[3]);

  map = new OpenLayers.Map("maps", {
    projection: new OpenLayers.Projection(options.epsg),
    displayProjection: new OpenLayers.Projection("EPSG:4326"),
    allOverlays: true,
    units: "m",
    numZoomLevels: 22,
    maxExtent: b,
  });

  map.addLayer(new OpenLayers.Layer.WMS(options.name, "/wms", {layers: options.name}));
  map.addControl(new OpenLayers.Control.Navigation());
  map.addControl(new OpenLayers.Control.LayerSwitcher());
  map.addControl(new OpenLayers.Control.PanZoom());
  map.zoomToExtent(b);

}; 
