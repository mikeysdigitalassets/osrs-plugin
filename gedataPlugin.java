package net.runelite.client.plugins.gedata;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.Client;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.inject.Inject;
import java.io.IOException;

@PluginDescriptor(
		name = "GEdata Plugin",
		description = "Tracks GE buy/sell offers and sends data to an API",
		tags = {"grand exchange", "tracker"}
)
public class gedataPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(gedataPlugin.class);
	private static final OkHttpClient httpClient = new OkHttpClient();
	private static final String API_BASE_URL = "http://localhost:5000/api/linked/";

	@Inject
	private Client client;

	@Override
	protected void startUp() throws Exception
	{
		log.info("GEdata Plugin started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("GEdata Plugin stopped!");
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		GrandExchangeOffer offer = event.getOffer();
		if (offer.getState() == GrandExchangeOfferState.BOUGHT || offer.getState() == GrandExchangeOfferState.SOLD)
		{
			sendToApi(offer);
		}
	}

	private void sendToApi(GrandExchangeOffer offer)
	{
		String itemName = client.getItemDefinition(offer.getItemId()).getName();
		String username = client.getLocalPlayer().getName();

		String json = String.format(
				"{\"username\": \"%s\", \"itemId\": %d, \"quantity\": %d, \"price\": %d, \"itemName\": \"%s\"}",
				username, offer.getItemId(), offer.getTotalQuantity(), offer.getPrice(), itemName
		);

		String endpoint = (offer.getState() == GrandExchangeOfferState.SOLD) ? "sell" : "tracker";
		String url = API_BASE_URL + username + "/" + endpoint;

		MediaType JSON = MediaType.parse("application/json");
		RequestBody body = RequestBody.create(JSON, json);
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.error("Failed to send data to API", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				if (!response.isSuccessful())
				{
					log.error("Unexpected code " + response);
				}
			}
		});
	}
}
