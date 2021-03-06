package org.labkey.response.data;

import org.labkey.api.data.Container;
import org.labkey.response.participantproperties.ParticipantProperty;

public class ParticipantPropertyMetadata
{
    private Integer rowId;
    private Integer listId;
    private String propertyURI;
    private ParticipantProperty.ParticipantPropertyType propertyType;
    private Container container;

    public void setListId(Integer listId)
    {
        this.listId = listId;
    }

    public Integer getListId()
    {
        return this.listId;
    }

    public void setRowId(Integer rowId)
    {
        this.rowId = rowId;
    }

    public Integer getRowId()
    {
        return this.rowId;
    }

    public void setContainer(Container container)
    {
        this.container = container;
    }

    public Container getContainer()
    {
        return this.container;
    }

    public void setPropertyType(ParticipantProperty.ParticipantPropertyType propertyType)
    {
        this.propertyType = propertyType;
    }

    public ParticipantProperty.ParticipantPropertyType getPropertyType()
    {
        return this.propertyType;
    }

    public void setPropertyURI(String propertyURI)
    {
        this.propertyURI = propertyURI;
    }

    public String getPropertyURI()
    {
        return this.propertyURI;
    }
}
