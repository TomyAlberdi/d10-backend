package d10.backend.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import d10.backend.DTO.Invoice.MonthlySummaryRecordDTO;
import d10.backend.Model.Invoice;
import d10.backend.Repository.InvoiceRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class DataService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Get yearly sales data with monthly summaries
     * @param year the year to get sales data for
     * @return list of monthly summaries with income = 0 for months without invoices
     */
    public List<MonthlySummaryRecordDTO> getYearlySalesData(Integer year) {
        // Create date range for the entire year
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31).plusDays(1);

        // Statuses to include in the query
        List<Invoice.Status> statuses = List.of(
            Invoice.Status.PAGO,
            Invoice.Status.ENVIADO,
            Invoice.Status.ENTREGADO
        );

        // Get invoices for the year with specified statuses
        List<Invoice> invoices = invoiceRepository.findByDateRangeAndStatus(startDate, endDate, statuses);

        // Initialize result with all months set to 0 income
        List<MonthlySummaryRecordDTO> monthlySummaries = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            monthlySummaries.add(new MonthlySummaryRecordDTO(
                month,
                year,
                BigDecimal.ZERO
            ));
        }

        // Calculate monthly totals from invoices
        for (Invoice invoice : invoices) {
            int month = invoice.getDate().getMonthValue();
            BigDecimal invoiceTotal = BigDecimal.valueOf(invoice.getTotal() != null ? invoice.getTotal() : 0.0);

            MonthlySummaryRecordDTO monthlySummary = monthlySummaries.get(month - 1);
            monthlySummary.setIncome(monthlySummary.getIncome().add(invoiceTotal));
        }

        return monthlySummaries;
    }

    /**
     * Get data analysis results from Product model
     */
    public Object getProductAnalysis() {
        // TODO: Implement product data analysis
        return null;
    }

    /**
     * Get data analysis results from Client model
     */
    public Object getClientAnalysis() {
        // TODO: Implement client data analysis
        return null;
    }

    /**
     * Get data analysis results from Invoice model
     */
    public Object getInvoiceAnalysis() {
        // TODO: Implement invoice data analysis
        return null;
    }

    /**
     * Get data analysis results from CashRegister model
     */
    public Object getCashRegisterAnalysis() {
        // TODO: Implement cash register data analysis
        return null;
    }

    /**
     * Get data analysis results from Warehouse model
     */
    public Object getWarehouseAnalysis() {
        // TODO: Implement warehouse data analysis
        return null;
    }

    /**
     * Get data analysis results from Provider model
     */
    public Object getProviderAnalysis() {
        // TODO: Implement provider data analysis
        return null;
    }

    /**
     * Get comprehensive data analysis from all models
     */
    public Object getComprehensiveAnalysis() {
        // TODO: Implement comprehensive data analysis
        return null;
    }
}
