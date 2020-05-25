package com.space.service;

import com.space.BadRequestException;
import com.space.ShipNotFoundException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ShipServiceImpl implements ShipService {


    private ShipRepository shipRepository;

    @Autowired
    public void setShipRepository(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @Override
    public Page<Ship> getAllShips(Specification<Ship> specification, Pageable sortedByName) {
        return shipRepository.findAll(specification, sortedByName);
    }

    @Override
    public List<Ship> getAllShips(Specification<Ship> specification) {
        return shipRepository.findAll(specification);
    }

    @Override
    public Ship createShip(Ship ship) {
        if (ship.getName() == null
                || ship.getPlanet() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null)
            throw new BadRequestException("One of Ship params is null");

        checkParameters(ship);

        if (ship.getUsed() == null) {
            ship.setUsed(false);
        }
        ship.setRating(calculateRating(ship));
        return shipRepository.saveAndFlush(ship);
    }

    @Override
    public Ship getShip(Long id) {
        if (!shipRepository.existsById(id))
            throw new ShipNotFoundException("Ship not found");

        return shipRepository.findById(id).get();
    }

    @Override
    public Ship editShip(Long id, Ship ship) {
        checkParameters(ship);

        if (!shipRepository.existsById(id))
            throw new ShipNotFoundException("Ship not found");

        Ship editedShip = shipRepository.findById(id).get();

        if (ship.getName() != null)
            editedShip.setName(ship.getName());

        if (ship.getPlanet() != null)
            editedShip.setPlanet(ship.getPlanet());

        if (ship.getShipType() != null)
            editedShip.setShipType(ship.getShipType());

        if (ship.getProdDate() != null)
            editedShip.setProdDate(ship.getProdDate());

        if (ship.getSpeed() != null)
            editedShip.setSpeed(ship.getSpeed());

        if (ship.getUsed() != null)
            editedShip.setUsed(ship.getUsed());

        if (ship.getCrewSize() != null)
            editedShip.setCrewSize(ship.getCrewSize());

        Double rating = calculateRating(editedShip);
        editedShip.setRating(rating);

        return shipRepository.save(editedShip);
    }

    @Override
    public void deleteShip(Long id) {
        if (!shipRepository.existsById(id))
            throw new ShipNotFoundException("Ship not found");
        shipRepository.deleteById(id);
    }

    @Override
    public Specification<Ship> filterByName(String name) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (name == null ) return null;
                return criteriaBuilder.like(root.get("name"), "%" + name + "%");
            }
        };
    }

    @Override
    public Specification<Ship> filterByPlanet(String planet) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (planet == null ) return null;
                return criteriaBuilder.like(root.get("planet"), "%" + planet + "%");
            }
        };
    }

    @Override
    public Specification<Ship> filterByShipType(ShipType shipType) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (shipType == null ) return null;
                return criteriaBuilder.equal(root.get("shipType"), shipType);
            }
        };
    }

    @Override
    public Specification<Ship> filterByDate(Long after, Long before) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (after == null && before == null)
                    return null;
                if (after == null) {
                    Date dateBefore = new Date(before);
                    return criteriaBuilder.lessThanOrEqualTo(root.get("prodDate"), dateBefore);
                }
                if (before == null) {
                    Date dateAfter = new Date(after);
                    return criteriaBuilder.greaterThanOrEqualTo(root.get("prodDate"), dateAfter);
                }
                Date dateBefore = new Date(before);
                Date dateAfter = new Date(after);
                return criteriaBuilder.between(root.get("prodDate"), dateAfter, dateBefore);
            }
        };
    }

    @Override
    public Specification<Ship> filterByUsage(Boolean isUsed) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (isUsed == null) {
                    return null;
                } else if (isUsed) {
                    return criteriaBuilder.isTrue(root.get("isUsed"));
                } else {
                    return criteriaBuilder.isFalse(root.get("isUsed"));
                }
            }
        };
    }

    @Override
    public Specification<Ship> filterBySpeed(Double min, Double max) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (min == null && max == null)
                    return null;
                if (min == null)
                    return criteriaBuilder.lessThanOrEqualTo(root.get("speed"), max);
                if (max == null)
                    return criteriaBuilder.greaterThanOrEqualTo(root.get("speed"), min);

                return criteriaBuilder.between(root.get("speed"), min, max);
            }
        };
    }

    @Override
    public Specification<Ship> filterByCrewSize(Integer min, Integer max) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (min == null && max == null)
                    return null;
                if (min == null)
                    return criteriaBuilder.lessThanOrEqualTo(root.get("crewSize"), max);
                if (max == null)
                    return criteriaBuilder.greaterThanOrEqualTo(root.get("crewSize"), min);

                return criteriaBuilder.between(root.get("crewSize"), min, max);
            }
        };
    }

    @Override
    public Specification<Ship> filterByRating(Double min, Double max) {
        return new Specification<Ship>() {
            @Override
            public Predicate toPredicate(Root<Ship> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                if (min == null && max == null)
                    return null;
                if (min == null)
                    return criteriaBuilder.lessThanOrEqualTo(root.get("rating"), max);
                if (max == null)
                    return criteriaBuilder.greaterThanOrEqualTo(root.get("rating"), min);

                return criteriaBuilder.between(root.get("rating"), min, max);
            }
        };
    }

    private void checkParameters(Ship ship) {

        if (ship.getName() != null && (ship.getName().length() < 1 || ship.getName().length() > 50))
            throw new BadRequestException("Incorrect Ship.name");

        if (ship.getPlanet() != null && (ship.getPlanet().length() < 1 || ship.getPlanet().length() > 50))
            throw new BadRequestException("Incorrect Ship.planet");

        if (ship.getCrewSize() != null && (ship.getCrewSize() < 1 || ship.getCrewSize() > 9999))
            throw new BadRequestException("Incorrect Ship.crewSize");

        if (ship.getSpeed() != null && (ship.getSpeed() < 0.01D || ship.getSpeed() > 0.99D))
            throw new BadRequestException("Incorrect Ship.speed");

        if (ship.getProdDate() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(ship.getProdDate());
            if (cal.get(Calendar.YEAR) < 2800 || cal.get(Calendar.YEAR) > 3019)
                throw new BadRequestException("Incorrect Ship.date");
        }
    }

    private Double calculateRating(Ship ship) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(ship.getProdDate());
        int prodYear = cal.get(Calendar.YEAR);
        double v = ship.getSpeed();
        double k = ship.getUsed() ? 0.5d : 1;
        int currentYear = 3019;
        double rating = (80.0d * v * k) / (double) (currentYear - prodYear + 1);
        BigDecimal result = new BigDecimal(rating);
        return result.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    @Override
    public Long checkAndParseId(String id) {
        if (id == null || id.equals("") || id.equals("0"))
            throw new BadRequestException("Некорректный ID");

        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID не является числом", e);
        }
    }

}
