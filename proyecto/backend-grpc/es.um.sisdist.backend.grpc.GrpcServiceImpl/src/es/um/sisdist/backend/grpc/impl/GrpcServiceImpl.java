package es.um.sisdist.backend.grpc.impl;

import java.util.logging.Logger;

import es.um.sisdist.backend.grpc.GrpcServiceGrpc;
import es.um.sisdist.backend.grpc.PingRequest;
import es.um.sisdist.backend.grpc.PingResponse;
import io.grpc.stub.StreamObserver;
import es.um.sisdist.backend.dao.DAOFactoryImpl;
import es.um.sisdist.backend.dao.IDAOFactory;
import es.um.sisdist.backend.dao.conversations.IConversationsDAO;
import es.um.sisdist.backend.dao.user.IUserDAO;
import es.um.sisdist.backend.grpc.*;

import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import org.json.JSONObject;

class GrpcServiceImpl extends GrpcServiceGrpc.GrpcServiceImplBase 
{

	private final Map<String, String> taskStatus = new ConcurrentHashMap<>();
    private final Map<String, String> taskAnswer = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

	private Logger logger;
	IDAOFactory daoFactory;
	private final IConversationsDAO conversationsDAO;
	
    public GrpcServiceImpl(Logger logger) 
    {
		super();
		this.logger = logger;
		daoFactory = new DAOFactoryImpl();
		this.conversationsDAO = daoFactory.createConversationsDAO();
	}

	@Override
	public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) 
	{
		logger.info("Recived PING request, value = " + request.getV());
		responseObserver.onNext(PingResponse.newBuilder().setV(request.getV()).build());
		responseObserver.onCompleted();
	}

	@Override
    public void sendPrompt(PromptRequest request, StreamObserver<PromptResponse> responseObserver) {
        String token = UUID.randomUUID().toString();
        taskStatus.put(token, "processing");
        String email = request.getEmail();
        String name = request.getName();
        String prompt = request.getPrompt();

        executor.submit(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String json = new JSONObject().put("prompt", prompt).toString();
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://ssdd-llamachat:5020/prompt"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                boolean done = false;
                while (!done) {
                    HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                    int statusCode = httpResponse.statusCode();
                    logger.info("LlamaChat POST /prompt status: " + statusCode);

                    if (statusCode == 102) {
                        logger.info("LlamaChat aún no está listo (102), reintentando en 1s...");
                        Thread.sleep(1000);
                        continue;
                    } else if (statusCode == 202) {
                        // Usa la cabecera Location que devuelve LlamaChat
                        String location = httpResponse.headers().firstValue("Location").orElse("");
                        if (!location.startsWith("http")) {
                            location = "http://ssdd-llamachat:5020" + location;
                        }
                        logger.info("LlamaChat aceptó el prompt (202), Location: " + location);
                        // Polling loop
                        while (true) {
                            Thread.sleep(1000);
                            HttpRequest pollRequest = HttpRequest.newBuilder()
                                    .uri(URI.create(location))
                                    .GET()
                                    .build();
                            HttpResponse<String> pollResponse = client.send(pollRequest, HttpResponse.BodyHandlers.ofString());
                            int pollStatus = pollResponse.statusCode();
                            logger.info("LlamaChat GET (polling) status: " + pollStatus);
                            if (pollStatus == 200) {
                                JSONObject obj = new JSONObject(pollResponse.body());
                                String answer = obj.optString("answer", "");
                                taskStatus.put(token, "ready");
                                taskAnswer.put(token, answer);
                                conversationsDAO.addResponse(email, name, prompt, answer, System.currentTimeMillis());
                                logger.info("Respuesta recibida y guardada en la base de datos.");
                                break;
                            } else if (pollStatus == 204) {
                                logger.info("LlamaChat aún procesando (204), esperando...");
                            } else if (pollStatus == 102) {
                                logger.info("LlamaChat aún procesando (102), esperando...");
                            } else {
                                logger.warning("Error inesperado en polling: " + pollStatus);
                                taskStatus.put(token, "error");
                                break;
                            }
                        }
                        done = true;
                    } else if (statusCode == 200) {
                        JSONObject obj = new JSONObject(httpResponse.body());
                        String answer = obj.optString("answer", "");
                        taskStatus.put(token, "ready");
                        taskAnswer.put(token, answer);
                        conversationsDAO.addResponse(email, name, prompt, answer, System.currentTimeMillis());
                        logger.info("Respuesta inmediata recibida y guardada en la base de datos.");
                        done = true;
                    } else {
                        logger.warning("Error inesperado en POST: " + statusCode);
                        taskStatus.put(token, "error");
                        done = true;
                    }
                }
            } catch (Exception e) {
                logger.severe("Excepción en sendPrompt: " + e.getMessage());
                taskStatus.put(token, "error");
            }
        });

        responseObserver.onNext(PromptResponse.newBuilder().setToken(token).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAnswer(PromptResultRequest request, StreamObserver<PromptResultResponse> responseObserver) {
        String token = request.getToken();
        String status = taskStatus.getOrDefault(token, "error");
        String answer = taskAnswer.getOrDefault(token, "");

        responseObserver.onNext(PromptResultResponse.newBuilder()
                .setStatus(status)
                .setAnswer(answer)
                .build());
        responseObserver.onCompleted();
    }


/*
	@Override
	public void storeImage(ImageData request, StreamObserver<Empty> responseObserver)
    {
		logger.info("Add image " + request.getId());
    	imageMap.put(request.getId(),request);
    	responseObserver.onNext(Empty.newBuilder().build());
    	responseObserver.onCompleted();
	}

	@Override
	public StreamObserver<ImageData> storeImages(StreamObserver<Empty> responseObserver) 
	{
		// La respuesta, sólo un objeto Empty
		responseObserver.onNext(Empty.newBuilder().build());

		// Se retorna un objeto que, al ser llamado en onNext() con cada
		// elemento enviado por el cliente, reacciona correctamente
		return new StreamObserver<ImageData>() {
			@Override
			public void onCompleted() {
				// Terminar la respuesta.
				responseObserver.onCompleted();
			}
			@Override
			public void onError(Throwable arg0) {
			}
			@Override
			public void onNext(ImageData imagedata) 
			{
				logger.info("Add image (multiple) " + imagedata.getId());
		    	imageMap.put(imagedata.getId(), imagedata);	
			}
		};
	}

	@Override
	public void obtainImage(ImageSpec request, StreamObserver<ImageData> responseObserver) {
		
		super.obtainImage(request, responseObserver);
	}

	@Override
	public StreamObserver<ImageSpec> obtainCollage(StreamObserver<ImageData> responseObserver) {
		
		return super.obtainCollage(responseObserver);
	}
	*/
}