package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Scanner;

public class Main {

    private static final String ISS_API_LOCATION = "http://api.open-notify.org/iss-now.json";
    private static final String ISS_API_PEOPLE = "http://api.open-notify.org/astros.json";

    public static void main(String[] args) throws IOException, InterruptedException {

        Scanner scanner = new Scanner(System.in);

        int choice;

        do {
            System.out.println("1. Pobierz położenie ISS");
            System.out.println("2. Pobierz ludzi na ISS");
            System.out.println("3. Pokaż prędkość ISS");
            System.out.println("4. Zakończ aplikacje");

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    //Sprawdź położenie ISS

                    //Stworzenie HTTP klienta, request i wysłanie requestu z rządaniem odpowiedzi
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(ISS_API_LOCATION))
                            .build();
                    final HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

                    //Tworzymy sobie mppera, żeby wciągnąć wartość z JSONa, czyli odpowiedzi z zewnętrznego serwisu
                    ObjectMapper objectMapper = new ObjectMapper();

                    final JsonNode jsonNode = objectMapper.readTree(send.body());

                    //Wyciągamy timestamp jako long
                    long timestamp = jsonNode.at("/timestamp").asLong();

                    //Tworzymy obiekt instant, który będzie nam potrzebny do stworzenia dalej obiektu LocalDataTiem
                    Instant instant = Instant.ofEpochSecond(timestamp);
                    LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

                    //Wyciągamy szerokość i długość ISS
                    final double lat = jsonNode.at("/iss_position/latitude").asDouble();
                    final double lon = jsonNode.at("/iss_position/longitude").asDouble();

                    System.out.println("Dnia " + localDateTime + " ISS " + " jest w miejscu szerokość: " + lat + " długość " + lon);

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("iss_location.csv", true))) {
                        StringBuilder line = new StringBuilder();
                        line.append("date").append(",").append(localDateTime).append(",").append("lat")
                                .append(",").append(lat).append(",").append("lon").append(",").append(lon).append("\n");
                        writer.write(line.toString());
                    }
                    break;

                case 2:
                    final var response1 = getHttpResponse();

                    ObjectMapper objectMapper1 = new ObjectMapper();
                    final JsonNode jsonNode1 = objectMapper1.readTree(response1.body());
                    final int totalNumber = jsonNode1.at("/number").asInt();

                    StringBuilder people = new StringBuilder();
                    for (JsonNode jsonArrayNode: jsonNode1.at("/people")) {
                        String name = jsonArrayNode.at("/name").asText();
                        System.out.println(name);
                        people.append(name).append(",");
                    }
                    people.append(totalNumber).append("\n");

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("iss_people.csv"))) {
                        writer.write(people.toString());
                    }

                    System.out.println("Wszystkich osób jest " + totalNumber);
                    break;

                case 3:
                    final HttpResponse<String> stringHttpResponseFirst = getStringHttpResponse(ISS_API_LOCATION);
                    final JsonNode jsonNodeFirst = getJsonNode(stringHttpResponseFirst);
                    final double latFirst = jsonNodeFirst.at("/iss_position/latitude").asDouble();
                    final double lonFirst = jsonNodeFirst.at("/iss_position/longitude").asDouble();

                    //TODO weź czas z timestampa
                    final int timeDifferenceInSeconds = 2;
                    Thread.sleep(Duration.ofSeconds(timeDifferenceInSeconds));

                    final HttpResponse<String> stringHttpResponseSecond = getStringHttpResponse(ISS_API_LOCATION);
                    final JsonNode jsonNodeSecond = getJsonNode(stringHttpResponseSecond);
                    final double latSecond = jsonNodeSecond.at("/iss_position/latitude").asDouble();
                    final double lonSecond = jsonNodeSecond.at("/iss_position/longitude").asDouble();

                    final double distance = calculateDistance(latFirst, lonFirst, latSecond, lonSecond);

                    //dorga/przez czas
                    double speed = distance / timeDifferenceInSeconds;
                    System.out.println("Iss is going " + speed + "km/s");
                    writeToCsv("iss_speed.csv", true, "Speed", String.valueOf(speed));
                    break;

                case 4:
                    System.out.println("Zamkykamy appkę");
                    break;

                default:
                    System.out.println("Nie ma takiej komendy");
                    break;
            }

        } while (choice != 4);

        scanner.close();
    }

    private static HttpResponse<String> getHttpResponse() throws IOException, InterruptedException {
        HttpClient client1 = HttpClient.newHttpClient();
        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create(ISS_API_PEOPLE))
                .build();

        final HttpResponse<String> response1 = client1.send(request1, HttpResponse.BodyHandlers.ofString());
        return response1;
    }


    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Promień ziemi w kilometrach
        final double r = 6371;

        // Różnice szerokości i długości geograficznych
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        // Obliczenia według wzoru haversine
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Odległość w kilometrach
        return r * c;
    }
}