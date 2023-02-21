import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.WordUtils;

public class EventbriteUnionTool {
  private List<CSVRecord> eventbrite;
  private List<CSVRecord> union;

  public EventbriteUnionTool(String eventbritePath, String... unionPaths) throws FileNotFoundException {
    try {
      eventbrite = parseExcelCSV(eventbritePath);
      union = new ArrayList<>();
      for (String unionPath : unionPaths) {
        union.addAll(parseExcelCSV(unionPath));
      }
    } catch (IOException e) {
      System.out.println("Parse error");
    }
  }

  private List<CSVRecord> parseExcelCSV(String filePath) throws IOException {
    CSVFormat.Builder builder = CSVFormat.EXCEL.builder();
    Reader reader = new FileReader(filePath);
    return builder.setHeader().setSkipHeaderRecord(true).build().parse(reader).getRecords();
  }

  public static void main(String[] args) {
    try {
      EventbriteUnionTool tool = new EventbriteUnionTool("../CGCU x DoCSoc boat party/orders.csv", "../CGCU x DoCSoc boat party/Purchase_Group_Report 46377.csv", "../CGCU x DoCSoc boat party/Purchase_Group_Report 46379.csv");
      System.out.println(tool.multipleOrders());
      System.out.println(tool.didNotPayOnUnion());
      System.out.println(tool.didNotReserveOnEventbrite());
    } catch (FileNotFoundException e) {
      System.out.println("One of the paths is wrong");
    }
  }

  public Set<Name> multipleOrders() {
    Map<Name, Integer> orders = new HashMap<>();
    for (CSVRecord record : eventbrite) {
      Name name = new Name(record);
      orders.put(name, orders.getOrDefault(name, 0) + 1);
    }
    Set<Name> multipleOrders = new HashSet<>();
    for (Name name : orders.keySet()) {
      if (orders.get(name) > 1) {
        multipleOrders.add(name);
      }
    }
    return multipleOrders;
  }

  public Set<Name> didNotPayOnUnion() {
    Set<Name> reservedOnEventbrite = getNamesFromCSVRecords(eventbrite);
    Set<Name> paidOnUnion = getNamesFromCSVRecords(union);
    reservedOnEventbrite.removeAll(paidOnUnion);
    return reservedOnEventbrite;
  }

  public Set<Name> didNotReserveOnEventbrite() {
    Set<Name> reservedOnEventbrite = getNamesFromCSVRecords(eventbrite);
    Set<Name> paidOnUnion = getNamesFromCSVRecords(union);
    paidOnUnion.removeAll(reservedOnEventbrite);
    return paidOnUnion;
  }

  private Set<Name> getNamesFromCSVRecords(List<CSVRecord> csvRecords) {
    return csvRecords.stream().map(Name::new).collect(Collectors.toSet());
  }

  private static class Name {
    private final String firstName;
    private final String surname;

    Name(CSVRecord csvRecord) {
      assert csvRecord.isMapped("First Name");
      assert csvRecord.isMapped("Surname");
      this.firstName = getAndFormatName(csvRecord, "First Name");
      this.surname = getAndFormatName(csvRecord, "Surname");
    }

    @Override
    public String toString() {
      return surname + " " + firstName;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Name other) {
        String otherFirstName = other.firstName.toLowerCase();
        String otherSurname = other.surname.toLowerCase();
        return otherFirstName.equals(firstName.toLowerCase()) && otherSurname.equals(surname.toLowerCase());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hash(firstName.toLowerCase(), surname.toLowerCase());
    }

    private String getAndFormatName(CSVRecord csvRecord, String tag) {
      return WordUtils.capitalizeFully(csvRecord.get(tag), ' ').trim();
    }
  }
}
