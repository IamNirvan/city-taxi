package com.citytaxi.city_taxi.services.impl;

import com.citytaxi.city_taxi.exceptions.BadRequestException;
import com.citytaxi.city_taxi.exceptions.NotFoundException;
import com.citytaxi.city_taxi.models.dtos.driver.request.DriverCreateRequest;
import com.citytaxi.city_taxi.models.dtos.driver.request.DriverUpdateRequest;
import com.citytaxi.city_taxi.models.dtos.driver.response.*;
import com.citytaxi.city_taxi.models.dtos.email.SendRegistrationEmailRequest;
import com.citytaxi.city_taxi.models.dtos.rating.response.RatingGetResponse;
import com.citytaxi.city_taxi.models.dtos.vehicle.response.VehicleGetResponse;
import com.citytaxi.city_taxi.models.dtos.vehicle_type.response.VehicleTypeGetResponse;
import com.citytaxi.city_taxi.models.entities.Account;
import com.citytaxi.city_taxi.models.entities.Driver;
import com.citytaxi.city_taxi.models.entities.Vehicle;
import com.citytaxi.city_taxi.models.entities.VehicleType;
import com.citytaxi.city_taxi.models.enums.EAccountStatus;
import com.citytaxi.city_taxi.models.enums.EAccountType;
import com.citytaxi.city_taxi.models.enums.EDriverAvailabilityStatus;
import com.citytaxi.city_taxi.repositories.AccountRepository;
import com.citytaxi.city_taxi.repositories.DriverRepository;
import com.citytaxi.city_taxi.repositories.RatingRepository;
import com.citytaxi.city_taxi.repositories.VehicleRepository;
import com.citytaxi.city_taxi.services.IDriverService;
import com.citytaxi.city_taxi.util.PasswordGenerator;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

@Service
@Log4j2
@AllArgsConstructor
public class DriverService implements IDriverService {
    private final VehicleRepository vehicleRepository;
    private final AccountRepository accountRepository;
    private final DriverRepository driverRepository;
    private final RatingRepository ratingRepository;
    private final BlockingQueue<SendRegistrationEmailRequest> emailQueue;
    private final PasswordGenerator passwordGenerator;

    /**
     * Creates a list of drivers based on the provided payload.
     *
     * @param payload List of DriverCreateRequest objects containing driver details.
     * @return List of DriverCreateResponse objects containing created driver details.
     * @throws BadRequestException if a driver with the same email or phone number already exists.
     */
    @Override
    @Transactional
    public List<DriverCreateResponse> create(List<DriverCreateRequest> payload) throws BadRequestException {
        final List<DriverCreateResponse> response = new ArrayList<>();

        for (DriverCreateRequest request : payload) {
            if (driverRepository.existsByEmailOrPhoneNumber(request.getEmail(), request.getPhoneNumber())) {
                throw new BadRequestException("Driver with email or phone number already exists");
            }

            final Driver driver = Driver.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .driverLicense(request.getDriverLicense())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .locationName(request.getLocationName())
                .availability(EDriverAvailabilityStatus.AVAILABLE)
                .createdAt(OffsetDateTime.now())
                .build();

            driverRepository.save(driver);
            log.debug("driver created");

            response.add(DriverCreateResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .driverLicense(driver.getDriverLicense())
                .availability(driver.getAvailability())
                .latitude(driver.getLatitude())
                .longitude(driver.getLongitude())
                .locationName(driver.getLocationName())
                .createdAt(driver.getCreatedAt())
                .build());
        }

        return response;
    }

    /**
     * Updates a list of drivers based on the provided payload.
     *
     * @param payload List of DriverUpdateRequest objects containing updated driver details.
     * @return List of DriverUpdateResponse objects containing updated driver details.
     * @throws NotFoundException if a driver with the specified ID is not found.
     * @throws BadRequestException if a driver with the same email, phone number, or license number already exists.
     */
    @Override
    @Transactional
    public List<DriverUpdateResponse> update(List<DriverUpdateRequest> payload) throws NotFoundException, BadRequestException {
        final List<DriverUpdateResponse> response = new ArrayList<>();

        for (DriverUpdateRequest request : payload) {
            final Driver driver = driverRepository.findById(request.getId()).orElseThrow(
                    () -> new NotFoundException(String.format("Driver with ID %d not found", request.getId()))
            );

            if (request.getName() != null) {
                driver.setName(request.getName());
            }

            if (request.getEmail() != null) {
                if (driverRepository.existsByEmail(request.getId(), request.getEmail())) {
                    throw new BadRequestException("Driver with email already exists");
                }
                driver.setEmail(request.getEmail());
            }

            if (request.getPhoneNumber() != null) {
                if (driverRepository.existsByPhoneNumber(request.getId(), request.getPhoneNumber())) {
                    throw new BadRequestException("Driver with phone number already exists");
                }
                driver.setPhoneNumber(request.getPhoneNumber());
            }

            if (request.getDriverLicense() != null) {
                if (driverRepository.existsByDriverLicense(request.getId(), request.getDriverLicense())) {
                    throw new BadRequestException("Driver with license number already exists");
                }
                driver.setDriverLicense(request.getDriverLicense());
            }

            if (request.getAvailability() != null) {
                driver.setAvailability(request.getAvailability());
            }

            if (request.getLocationName() != null) {
                driver.setLocationName(request.getLocationName());
            }

            if (request.getLatitude() != null) {
                driver.setLatitude(request.getLatitude());
            }

            if (request.getLongitude() != null) {
                driver.setLongitude(request.getLongitude());
            }

            driver.setUpdatedAt(OffsetDateTime.now());
            driverRepository.save(driver);
            log.debug("driver updated");

            response.add(DriverUpdateResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .driverLicense(driver.getDriverLicense())
                .latitude(driver.getLatitude())
                .longitude(driver.getLongitude())
                .locationName(driver.getLocationName())
                .availability(driver.getAvailability())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build());
        }
        return response;
    }

    /**
     * Deletes a list of drivers based on the provided IDs.
     *
     * @param ids List of driver IDs to be deleted.
     * @return List of DriverDeleteResponse objects containing deleted driver details.
     * @throws NotFoundException if a driver with the specified ID is not found.
     */
    @Override
    @Transactional
    public List<DriverDeleteResponse> delete(List<Long> ids) throws NotFoundException {
        final List<DriverDeleteResponse> response;
        response = new ArrayList<>();

        for (Long id : ids) {
            final Driver driver = driverRepository.findById(id).orElseThrow(
                    () -> new NotFoundException(String.format("Driver with ID %d not found", id))
            );

            driverRepository.delete(driver);

            response.add(DriverDeleteResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .driverLicense(driver.getDriverLicense())
                .availability(driver.getAvailability())
                .latitude(driver.getLatitude())
                .longitude(driver.getLongitude())
                .locationName(driver.getLocationName())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build());
        }

        log.debug("driver(s) deleted");
        return response;
    }

    /**
     * Retrieves a list of drivers. If an ID is provided, retrieves the driver with the specified ID.
     *
     * @param id Optional driver ID to retrieve a specific driver.
     * @return List of DriverGetResponse objects containing driver details.
     * @throws NotFoundException if a driver with the specified ID is not found.
     */
    @Override
    public List<DriverGetResponse> getDrivers(Long id) throws NotFoundException {
        if (id != null) {
            Driver driver = driverRepository.findById(id).orElseThrow(
                    () -> new NotFoundException(String.format("Driver with ID %d not found", id))
            );
            return List.of(generateDriverGetResponse(driver));
        }

        List<DriverGetResponse> response = new ArrayList<>();
        List<Driver> drivers = driverRepository.findAll();
        for (Driver driver : drivers) {
            response.add(generateDriverGetResponse(driver));
        }
        return response;
    }

    /**
     * Registers a new driver based on the provided payload.
     *
     * @param payload DriverCreateRequest object containing driver details.
     * @return DriverRegistrationResponse object containing registered driver details.
     * @throws BadRequestException if a driver with the same email or phone number already exists.
     */
    @Override
    public DriverRegistrationResponse registerDriver(DriverCreateRequest payload) {
        if (driverRepository.existsByEmailOrPhoneNumber(payload.getEmail(), payload.getPhoneNumber())) {
            throw new BadRequestException("Driver with email or phone number already exists");
        }

        final String password = passwordGenerator.generatePassword(8);

        // Create the account instance
        Account account = Account.builder()
                .username(payload.getEmail())
                .password(password)
                .status(EAccountStatus.ACTIVE)
                .accountType(EAccountType.DRIVER)
                .createdAt(OffsetDateTime.now())
                .build();
        accountRepository.save(account);

        // Create the driver instance
        Driver driver = Driver.builder()
                .name(payload.getName())
                .email(payload.getEmail())
                .phoneNumber(payload.getPhoneNumber())
                .driverLicense(payload.getDriverLicense())
                .availability(EDriverAvailabilityStatus.AVAILABLE)
                .latitude(payload.getLatitude())
                .longitude(payload.getLongitude())
                .locationName(payload.getLocationName())
                .account(account)
                .createdAt(OffsetDateTime.now())
                .build();

        driverRepository.save(driver);
        log.debug("driver registered");

        // Send registration email
        SendRegistrationEmailRequest emailRequest = SendRegistrationEmailRequest.builder()
                .to(payload.getEmail())
                .subject("City Taxi Registration")
                .username(payload.getEmail())
                .password(password)
                .build();
        emailQueue.add(emailRequest);

        return DriverRegistrationResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .driverLicense(driver.getDriverLicense())
                .username(account.getUsername())
                .password(account.getPassword())
                .availability(driver.getAvailability())
                .latitude(driver.getLatitude())
                .longitude(driver.getLongitude())
                .locationName(driver.getLocationName())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build();
    }

    /**
     * Retrieves a list of nearby drivers based on the provided customer location and radius.
     *
     * @param customerLat The latitude of the customer's location.
     * @param customerLng The longitude of the customer's location.
     * @param radius The radius within which to search for nearby drivers.
     * @return A list of DriverGetResponse objects containing the details of the nearby drivers.
     */
    @Override
    public List<DriverGetNearbyResponse> getNearbyDrivers(Double customerLat, Double customerLng, Double radius) {
        final List<Driver> drivers =  driverRepository.findNearbyDrivers(customerLat, customerLng, radius);
        final List<DriverGetNearbyResponse> response = new ArrayList<>();
        Vehicle vehicle;
        VehicleType vehicleType;
        List<RatingGetResponse> ratings;

        for (Driver driver : drivers) {
            vehicle = driver.getVehicles().get(0);
            vehicleType = vehicle.getVehicleType();
            ratings = ratingRepository.findAllByDriverId(driver.getId());

            response.add(DriverGetNearbyResponse.builder()
                .id(driver.getId())
                .name(driver.getName())
                .email(driver.getEmail())
                .phoneNumber(driver.getPhoneNumber())
                .driverLicense(driver.getDriverLicense())
                .avgRating(calculateAverageRating(ratings.stream().map(RatingGetResponse::getRating).toList()))
                .availability(driver.getAvailability())
                .latitude(driver.getLatitude())
                .longitude(driver.getLongitude())
                .locationName(driver.getLocationName())
                .vehicle(VehicleGetResponse.builder()
                    .id(vehicle.getId())
                    .manufacturer(vehicle.getManufacturer())
                    .model(vehicle.getModel())
                    .colour(vehicle.getColour())
                    .licensePlate(vehicle.getLicensePlate())
                    .vehicleType(VehicleTypeGetResponse.builder()
                        .id(vehicleType.getId())
                        .name(vehicleType.getName())
                        .pricePerMeter(vehicleType.getPricePerMeter())
                        .seatCount(vehicleType.getSeatCount())
                        .createdAt(vehicleType.getCreatedAt())
                        .updatedAt(vehicleType.getUpdatedAt())
                        .build())
                    .createdAt(vehicle.getCreatedAt())
                    .updatedAt(vehicle.getUpdatedAt())
                    .build())
                .createdAt(driver.getCreatedAt())
                .updatedAt(driver.getUpdatedAt())
                .build());
        }
        return response;
    }

    /**
     * Generates a response object containing driver details.
     *
     * @param driver The driver entity for which the response is generated.
     * @return A DriverGetResponse object containing the driver's details.
     */
    private DriverGetResponse generateDriverGetResponse(Driver driver) {
        List<RatingGetResponse> ratings = ratingRepository.findAllByDriverId(driver.getId());
        List<Vehicle> vehicles = vehicleRepository.findAllByDriverId(driver.getId());

        List<VehicleGetResponse> vehicleGetResponses = new ArrayList<>();
        VehicleType vehicleType = null;
        for (Vehicle vehicle : vehicles) {
            vehicleType = vehicle.getVehicleType();

            vehicleGetResponses.add(VehicleGetResponse.builder()
                    .id(vehicle.getId())
                            .manufacturer(vehicle.getManufacturer())
                            .model(vehicle.getModel())
                            .colour(vehicle.getColour())
                            .licensePlate(vehicle.getLicensePlate())
                            .vehicleType(vehicleType != null ? VehicleTypeGetResponse.builder()
                                    .id(vehicleType.getId())
                                    .name(vehicleType.getName())
                                    .pricePerMeter(vehicleType.getPricePerMeter())
                                    .seatCount(vehicleType.getSeatCount())
                                    .createdAt(vehicleType.getCreatedAt())
                                    .updatedAt(vehicleType.getUpdatedAt())
                                    .build() : null)
                            .createdAt(vehicle.getCreatedAt())
                            .updatedAt(vehicle.getUpdatedAt())
                    .build());
        }

        return DriverGetResponse.builder()
            .id(driver.getId())
            .name(driver.getName())
            .email(driver.getEmail())
            .phoneNumber(driver.getPhoneNumber())
            .driverLicense(driver.getDriverLicense())
            .avgRating(calculateAverageRating(ratings.stream().map(RatingGetResponse::getRating).toList()))
            .availability(driver.getAvailability())
            .latitude(driver.getLatitude())
            .longitude(driver.getLongitude())
            .locationName(driver.getLocationName())
            .vehicles(vehicleGetResponses)
            .createdAt(driver.getCreatedAt())
            .updatedAt(driver.getUpdatedAt())
            .build();
    }

    /**
     * Calculates the average rating from a list of ratings.
     *
     * @param ratings A list of integer ratings.
     * @return The average rating, clamped between 1 and 5.
     */
    private Integer calculateAverageRating(List<Integer> ratings) {
        if (ratings.isEmpty()) {
            return 0;
        }

        int sum = 0;
        for (Integer rating : ratings) {
            sum += rating;
        }
        return Math.max(1, Math.min((sum / ratings.size()), 5));
    }
}
