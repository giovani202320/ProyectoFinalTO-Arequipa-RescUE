package com.arequiparescue.emergency.presentation;

import com.arequiparescue.emergency.application.PublishTriageRuleVersionCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * Formulario para que el Administrador (ACT-03) publique y active una nueva versión inmutable
 * del catálogo de reglas de clasificación (HU-04, DEC-016, RN-EME-011).
 */
public class PublishTriageRuleVersionForm {

    private String incAPriority = "HIGH";
    private String incAEquipment = "SPECIALIZED";

    private String incBPriority = "MEDIUM";
    private String incBEquipment = "BASIC";

    private String incCPriority = "LOW";
    private String incCEquipment = "BASIC";

    private String incDPriority = "HIGH";
    private String incDEquipment = "BASIC";

    private List<String> customCategories;
    private List<String> customPriorities;
    private List<String> customEquipments;

    public PublishTriageRuleVersionForm() {
    }

    public List<PublishTriageRuleVersionCommand.RuleDraftItem> toDraftItems() {
        if (customCategories != null) {
            List<PublishTriageRuleVersionCommand.RuleDraftItem> items = new ArrayList<>();
            for (int i = 0; i < customCategories.size(); i++) {
                String category = customCategories.get(i);
                String priority = (customPriorities != null && i < customPriorities.size())
                        ? customPriorities.get(i)
                        : "";
                String equipment = (customEquipments != null && i < customEquipments.size())
                        ? customEquipments.get(i)
                        : "";
                items.add(new PublishTriageRuleVersionCommand.RuleDraftItem(category, priority, equipment));
            }
            return items;
        }
        return List.of(
                new PublishTriageRuleVersionCommand.RuleDraftItem("INC-A", incAPriority, incAEquipment),
                new PublishTriageRuleVersionCommand.RuleDraftItem("INC-B", incBPriority, incBEquipment),
                new PublishTriageRuleVersionCommand.RuleDraftItem("INC-C", incCPriority, incCEquipment),
                new PublishTriageRuleVersionCommand.RuleDraftItem("INC-D", incDPriority, incDEquipment)
        );
    }

    public String getIncAPriority() {
        return incAPriority;
    }

    public void setIncAPriority(String incAPriority) {
        this.incAPriority = incAPriority;
    }

    public String getIncAEquipment() {
        return incAEquipment;
    }

    public void setIncAEquipment(String incAEquipment) {
        this.incAEquipment = incAEquipment;
    }

    public String getIncBPriority() {
        return incBPriority;
    }

    public void setIncBPriority(String incBPriority) {
        this.incBPriority = incBPriority;
    }

    public String getIncBEquipment() {
        return incBEquipment;
    }

    public void setIncBEquipment(String incBEquipment) {
        this.incBEquipment = incBEquipment;
    }

    public String getIncCPriority() {
        return incCPriority;
    }

    public void setIncCPriority(String incCPriority) {
        this.incCPriority = incCPriority;
    }

    public String getIncCEquipment() {
        return incCEquipment;
    }

    public void setIncCEquipment(String incCEquipment) {
        this.incCEquipment = incCEquipment;
    }

    public String getIncDPriority() {
        return incDPriority;
    }

    public void setIncDPriority(String incDPriority) {
        this.incDPriority = incDPriority;
    }

    public String getIncDEquipment() {
        return incDEquipment;
    }

    public void setIncDEquipment(String incDEquipment) {
        this.incDEquipment = incDEquipment;
    }

    public List<String> getCustomCategories() {
        return customCategories;
    }

    public void setCustomCategories(List<String> customCategories) {
        this.customCategories = customCategories;
    }

    public List<String> getCustomPriorities() {
        return customPriorities;
    }

    public void setCustomPriorities(List<String> customPriorities) {
        this.customPriorities = customPriorities;
    }

    public List<String> getCustomEquipments() {
        return customEquipments;
    }

    public void setCustomEquipments(List<String> customEquipments) {
        this.customEquipments = customEquipments;
    }
}
