/*
 *  All rights reserved.
 */
package eu.rbecker.jsepa.transfer;

import eu.rbecker.jsepa.directdebit.util.SepaXmlDocumentBuilder;
import eu.rbecker.jsepa.directdebit.xml.schema.pain_001_001_03.*;

import javax.xml.datatype.DatatypeConfigurationException;
import java.io.StringWriter;
import java.util.GregorianCalendar;

/**
 *
 * @author Robert Becker <robert at rbecker.eu>
 */
class SepaTransferDocumentBuilder extends SepaXmlDocumentBuilder {

    public static String toXml(SepaTransferDocumentData source) throws DatatypeConfigurationException {
        Document doc = new Document();
        CustomerCreditTransferInitiationV03 transferData = new CustomerCreditTransferInitiationV03();
        doc.setCstmrCdtTrfInitn(transferData);

        transferData.setGrpHdr(createGroupHeaderSdd(source));

        transferData.getPmtInf().add(createPaymentInstructions(source));

        StringWriter resultWriter = new StringWriter();
        marshal(doc.getClass().getPackage().getName(), new ObjectFactory().createDocument(doc), resultWriter);
        return resultWriter.toString();
    }

    private static GroupHeader32 createGroupHeaderSdd(SepaTransferDocumentData data) {
        GroupHeader32 result = new GroupHeader32();
        // message id
        result.setMsgId(data.getDocumentMessageId());

        // created on
        result.setCreDtTm(calendarToXmlGregorianCalendarDateTime(GregorianCalendar.getInstance()));

        // number of tx
        result.setNbOfTxs(String.valueOf(data.getPayments().size()));

        // control sum
        result.setCtrlSum(data.getTotalPaymentSum());

        // creditor name
        PartyIdentification32 partyIdentification32 = new PartyIdentification32();
        partyIdentification32.setNm(data.getPayerName());

        result.setInitgPty(partyIdentification32);

        return result;
    }

    private static PaymentInstructionInformation3 createPaymentInstructions(SepaTransferDocumentData data) {
        PaymentInstructionInformation3 result = new PaymentInstructionInformation3();
        result.setBtchBookg(data.isBatchBooking());
        result.setChrgBr(ChargeBearerType1Code.SLEV);
        result.setCtrlSum(data.getTotalPaymentSum());
        result.setNbOfTxs(String.valueOf(data.getPayments().size()));

        setPayerName(data, result);

        setPayerIbanAndBic(data, result);

        result.setPmtInfId(data.getDocumentMessageId());
        result.setPmtMtd(PaymentMethod3Code.TRF);
        result.setReqdExctnDt(calendarToXmlGregorianCalendarDateTime(data.getDateOfExecution()));

        setPaymentTypeInformation(result);

        for (SepaTransferPayment p : data.getPayments()) {
            addPaymentData(result, p);
        }

        return result;
    }

    private static void addPaymentData(PaymentInstructionInformation3 result, SepaTransferPayment p) {
        result.getCdtTrfTxInf().add(createPaymentData(p));
    }

    private static void setPayerName(SepaTransferDocumentData data, PaymentInstructionInformation3 result) {
        PartyIdentification32 pi2 = new PartyIdentification32();
        pi2.setNm(data.getPayerName());
        result.setDbtr(pi2);
    }

    private static void setPayerIbanAndBic(SepaTransferDocumentData data, PaymentInstructionInformation3 result) {
        AccountIdentification4Choice ai = new AccountIdentification4Choice();
        ai.setIBAN(data.getPayerIban());
        CashAccount16 ca1 = new CashAccount16();
        ca1.setId(ai);
        result.setDbtrAcct(ca1);

        BranchAndFinancialInstitutionIdentification4 bafii = new BranchAndFinancialInstitutionIdentification4();
        FinancialInstitutionIdentification7 fii = new FinancialInstitutionIdentification7();
        fii.setBIC(data.getPayerBic());
        bafii.setFinInstnId(fii);
        result.setDbtrAgt(bafii);
    }

    private static void setPaymentTypeInformation(PaymentInstructionInformation3 result) {
        PaymentTypeInformation19 pti = new PaymentTypeInformation19();
        ServiceLevel8Choice sls = new ServiceLevel8Choice();
        sls.setCd("SEPA");
        pti.setSvcLvl(sls);
        result.setPmtTpInf(pti);
    }

    private static CreditTransferTransactionInformation10 createPaymentData(SepaTransferPayment p) {
        CreditTransferTransactionInformation10 result = new CreditTransferTransactionInformation10();
        setPaymentCurrencyAndSum(p, result);
        setPayeeName(p, result);
        setPayeeIbanAndBic(p, result);
        setEndToEndId(p, result);
        setReasonForPayment(p, result);

        return result;
    }

    private static void setPaymentCurrencyAndSum(SepaTransferPayment p, CreditTransferTransactionInformation10 result) {
        AmountType3Choice at = new AmountType3Choice();
        ActiveOrHistoricCurrencyAndAmount aohcaa = new ActiveOrHistoricCurrencyAndAmount();
        aohcaa.setCcy("EUR");
        aohcaa.setValue(p.getPaymentSum());
        at.setInstdAmt(aohcaa);
        result.setAmt(at);
    }

    private static void setPayeeName(SepaTransferPayment p, CreditTransferTransactionInformation10 result) {
        PartyIdentification32 pis2 = new PartyIdentification32();
        pis2.setNm(p.getPayeeName());
        result.setCdtr(pis2);
    }

    private static void setEndToEndId(SepaTransferPayment p, CreditTransferTransactionInformation10 result) {
        PaymentIdentification1 pis = new PaymentIdentification1();
        String id = p.getEndToEndId();
        pis.setEndToEndId(id == null || id.isEmpty() ? "NOTPROVIDED" : id);
        result.setPmtId(pis);
    }

    private static void setReasonForPayment(SepaTransferPayment p, CreditTransferTransactionInformation10 result) {
        RemittanceInformation5 ri = new RemittanceInformation5();
        ri.getUstrd().add(p.getReasonForPayment());
        result.setRmtInf(ri);
    }

    private static void setPayeeIbanAndBic(SepaTransferPayment p, CreditTransferTransactionInformation10 ctti) {
        CashAccount16 ca = new CashAccount16();
        AccountIdentification4Choice ai = new AccountIdentification4Choice();
        ai.setIBAN(p.getPayeeIban());
        ca.setId(ai);
        ctti.setCdtrAcct(ca);

        BranchAndFinancialInstitutionIdentification4 bafiis = new BranchAndFinancialInstitutionIdentification4();
        FinancialInstitutionIdentification7 fii = new FinancialInstitutionIdentification7();
        fii.setBIC(p.getPayeeBic());
        bafiis.setFinInstnId(fii);
        ctti.setCdtrAgt(bafiis);
    }
}
