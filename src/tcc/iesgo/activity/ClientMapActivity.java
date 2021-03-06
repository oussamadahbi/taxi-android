package tcc.iesgo.activity;

import tcc.iesgo.activity.R;
import tcc.iesgo.overlay.CustomItemizedOverlay;
import tcc.iesgo.overlay.MyCustomLocationOverlay;
import tcc.iesgo.http.connection.HttpClientFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

public class ClientMapActivity extends MapActivity implements LocationListener {
	
	MapView mapView; //Mapa
	List<Overlay> mapOverlays; //Objetos do mapa
	MapController mc; //Controlador do mapa
	LocationManager lm;
	MyCustomLocationOverlay myLocationOverlay; //Overlay do usuário
	
	OverlayItem overlayitem;
	
	CustomItemizedOverlay itemizedOverlay;
	
	GeoPoint geoActual;
	GeoPoint geoTaxi;
	
	ProgressDialog progressDialog;
	Handler mHandler = new Handler();
	
	Drawable dTaxi;
	
	HttpClient httpclient = HttpClientFactory.getThreadSafeClient();
	
	TextView mapInfo;
	
	private int minLatitude, maxLatitude, minLongitude, maxLongitude;

	private String result;
	private String data[];
	
	private JSONObject jObject;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
			init();
		} catch (Exception e) {
			Toast.makeText(ClientMapActivity.this, getString(R.string.login_error_connection), Toast.LENGTH_SHORT).show();
		}
    }
    
    private void init() throws ClientProtocolException, IOException, JSONException{
		//Layout da aplicacao
		setContentView(R.layout.map);
		
		//Instancia o mapView da aplicacao
		mapView = (MapView) findViewById(R.id.mapview);
		//Controladores de zoom
		mapView.setBuiltInZoomControls(true);
		//Instancia de overlays para o mapa
		mapOverlays = mapView.getOverlays();

		//Controlador do mapa
		mc = mapView.getController();
		mc.setZoom(16);
		
		//Obtem uma instacia do servico de gerenciador de localizacao
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		
		//Ativa os updates para receber notificacoes de localizacao (periodicamente)
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 120000, 100f, ClientMapActivity.this);

		//Útimo local conhecido
		Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		// Cria um overlay mostrando a posicao do dispositivo
		myLocationOverlay = new MyCustomLocationOverlay(ClientMapActivity.this,mapView);
		// Habilita o ponto azul de localizacao na tela
		myLocationOverlay.enableMyLocation();
		// Habilita atualizacoes do sensor
		myLocationOverlay.enableCompass();
		
		// Adiciona o overlay no mapa
		mapOverlays.add(myLocationOverlay);
		
		dTaxi = ClientMapActivity.this.getResources().getDrawable(R.drawable.icon_taxi);

		mapInfo = (TextView) findViewById(R.id.map_info);
		
		showMap(location);
    }

	//Chamando quando a posição do gps é alterada
	@Override
	public void onLocationChanged(Location location) {
		try {
			showMap(location);
		} catch (JSONException e) {
			Toast.makeText(ClientMapActivity.this, getString(R.string.login_error_connection), Toast.LENGTH_SHORT).show();
		}
	}

	public void showMap(Location location) throws JSONException {
		if (location == null)
			location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); //Último local registrado

		minLatitude = Integer.MAX_VALUE;
		maxLatitude = Integer.MIN_VALUE;
		minLongitude = Integer.MAX_VALUE;
		maxLongitude = Integer.MIN_VALUE;

		//Rotina p/ atualizar os objetos do mapa
		Overlay obj = mapOverlays.get(0); //Posição atual do usuário
		mapOverlays.clear(); //Limpa Overlays do mapa
		mapOverlays.add(obj); //Adiciona o usuario no mapa
		mapView.invalidate(); //Atualiza o mapa
		//Fim rotina

		//GeoPoint da posição atual do usuário
		geoActual = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));

		String taxis = getTaxis(location); //Json

		int length = getJsonResult(taxis, "id").length;
		String[] ids = new String[length];
		ids = getJsonResult(taxis, "id");
		String[] distances = new String[length];
		distances = getJsonResult(taxis, "distance");
		String[] latitudes = new String[length];
		latitudes = getJsonResult(taxis, "latitude");
		String[] longitudes = new String[length];
		longitudes = getJsonResult(taxis, "longitude");
		String[] names = new String[length];
		names = getJsonResult(taxis, "name");
		String[] vehicles = new String[length];
		vehicles = getJsonResult(taxis, "vehicle");
		String[] plaques = new String[length];
		plaques = getJsonResult(taxis, "plaque");
		String[] licenses = new String[length];
		licenses = getJsonResult(taxis, "license");
		String[] langs =new String[length];
		langs = getJsonResult(taxis, "lang");
		
		DecimalFormat conv = new DecimalFormat("0.00");
		
		for(int i=0;i<length;i++){

			String[] languages = langs[i].split(",");
			String lang = "";
			for(int j=0;j<languages.length;j++){

				if(languages[j].equals("pt")){
					lang += "Português";
				} else if(languages[j].equals("en")){
					lang += ", Inglês";
				} else if(languages[j].equals("es")){
					lang += ", Espanhol";
				}
			}
			
			double lat = Double.parseDouble(latitudes[i]);
			double lng = Double.parseDouble(longitudes[i]);

			//GeoPoint da posição atual do taxista
			geoTaxi = new GeoPoint((int) (lat * 1E6),(int) (lng * 1E6));
			
			overlayitem = new OverlayItem(geoTaxi, "RG: "+ids[i]+" - "+names[i], "Descrição:\nDistância: "+conv.format(Double.parseDouble(distances[i])/1000)+
					" km\nVeículo: "+vehicles[i]+"\nPlaca: "+plaques[i]+"\nLicença nº: "+licenses[i]+"\nIdiomas Conhecidos: "+lang+"\n");
			
			itemizedOverlay = new CustomItemizedOverlay(dTaxi, getParent(), mapView);
			
			itemizedOverlay.addOverlay(overlayitem);
			
			mapOverlays.add(itemizedOverlay);
		}
		
		//Localização atual do usuário
		myLocationOverlay = new MyCustomLocationOverlay(ClientMapActivity.this,mapView);
		//Habilita o ponto azul de localizacao na tela
		myLocationOverlay.enableMyLocation();
		//Habilita atualizacoes do sensor
		myLocationOverlay.enableCompass();

		//Adiciona o overlay no mapa
		mapOverlays.add(myLocationOverlay);
		
		//Rotina p/ forçar que todos os objetos encontrados apareçam no mapa
        maxLatitude = Math.max(geoActual.getLatitudeE6(), maxLatitude);
        minLatitude = Math.min(geoActual.getLatitudeE6(), minLatitude);
        maxLongitude = Math.max(geoActual.getLongitudeE6(), maxLongitude);
        minLongitude = Math.min(geoActual.getLongitudeE6(), minLongitude);

        maxLatitude = Math.max(geoTaxi.getLatitudeE6(), maxLatitude);
        minLatitude = Math.min(geoTaxi.getLatitudeE6(), minLatitude);
        maxLongitude = Math.max(geoTaxi.getLongitudeE6(), maxLongitude);
        minLongitude = Math.min(geoTaxi.getLongitudeE6(), minLongitude);
        
        mc.animateTo(new GeoPoint((maxLatitude + minLatitude)/2, (maxLongitude + minLongitude)/2 ));
        mc.zoomToSpan(Math.abs(maxLatitude - minLatitude), Math.abs(maxLongitude - minLongitude));

        Integer zoomlevel = mapView.getZoomLevel();
        Integer zoomlevel2 = zoomlevel - 1;
        mc.setZoom(zoomlevel2);
        //Fim rotina

		try {
			updateClientLocation(location); //Atualiza a posição do cliente
		} catch (Exception e) {
			Toast.makeText(ClientMapActivity.this, getString(R.string.login_error_connection), Toast.LENGTH_SHORT).show();
		}

		mapInfo.setText("O táxi mais próximo de você está a: "+conv.format(Double.parseDouble(distances[0])/1000)+ " km");
	}
	
	//Busca os táxis próximos a posicao informada
	public String getTaxis(Location location) {
		if (location != null) {
			try {
				HttpPost post = new HttpPost(getString(R.string.url_webservice) + getString(R.string.url_get_taxis) 
						+ location.getLatitude() + "/" + location.getLongitude() + "/" + getString(R.string.form_id_get_taxis));
				
				HttpResponse rp = httpclient.execute(post);
			
				if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
					this.result = EntityUtils.toString(rp.getEntity());
			} catch (Exception e) {
				Toast.makeText(ClientMapActivity.this, getString(R.string.login_error_connection), Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
		}
		return this.result;
	}
	
	//Atualiza a posição atual do usuário no webservice
	private void updateClientLocation(Location location) throws ClientProtocolException, IOException{
		if (location != null){
			
			HttpPost httppost = new HttpPost(getString(R.string.url_webservice) + getString(R.string.url_update_user_location)
					+ location.getLatitude() + "/" + location.getLongitude() + "/" + getString(R.string.form_id_update_loc));

			httpclient.execute(httppost);
		}
	}
	
	private String[] getJsonResult(String response, String option) throws JSONException{
		jObject = new JSONObject(response);
		JSONArray jArray = jObject.getJSONArray("taxis");
		data = new String[jArray.length()];
		for(int i=0;i<jArray.length();i++){
			this.data[i] = jArray.getJSONObject(i)
					.getString(option).toString();
		}
		return this.data;
	}
	
	//Chamado quando o GPS esta desativado (abre as conf. do GPS)
	@Override
	public void onProviderDisabled(String arg0) {
		final AlertDialog.Builder dialog = new AlertDialog.Builder(getParent());
		dialog.setTitle(getString(R.string.gps_disabled));
		dialog.setMessage(getString(R.string.gps_disabled_message));
		dialog.setIcon(R.drawable.gps_enable);
		dialog.setCancelable(false);
		
		dialog.setPositiveButton(getString(R.string.ad_button_positive),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,	int id) {
						Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivity(intent);
					}
				});
			
		dialog.setNegativeButton(
				getString(R.string.ad_button_negative), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,	int id) {
						dialog.cancel();
		                finish();
					}
		});
		dialog.show();
	}

	//Chamado quando o GPS esta ativado
	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
	}

	//Chamado quando o status do GPS e alterado
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
        case LocationProvider.OUT_OF_SERVICE:
                //Log.v(tag, "Status alterado: Fora de serviço");
                break;
        case LocationProvider.TEMPORARILY_UNAVAILABLE:
                //Log.v(tag, "Status alterado: Temporariamente indisponível");
                break;
        case LocationProvider.AVAILABLE:
                //Log.v(tag, "Status alterado: Disponível");
                break;
        }
	}

	//Retornar true se a aplicação estiver traçando rotas ou violara os termos de uso
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	// Chamado quando a activity comeca a interagir com o usuario
	@Override
	protected void onResume() {
		super.onResume();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 120000, 100f, ClientMapActivity.this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		PendingIntent origPendingIntent = PendingIntent.getBroadcast(getBaseContext(),0, getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
		lm.removeUpdates(origPendingIntent);
	}
	
	//Chamado quando a activity ja nao e visivel para o usuario
	@Override
	protected void onStop() {
		super.onStop();
		PendingIntent origPendingIntent = PendingIntent.getBroadcast(getBaseContext(),0, getIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
		lm.removeUpdates(origPendingIntent);
		//System.exit(0);
	}
}