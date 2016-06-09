package uk.gov.justice.services.example.cakeshop.event.listener;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.example.cakeshop.domain.event.RecipeAdded;
import uk.gov.justice.services.example.cakeshop.event.listener.converter.RecipeAddedToIngredientsConverter;
import uk.gov.justice.services.example.cakeshop.event.listener.converter.RecipeAddedToRecipeConverter;
import uk.gov.justice.services.example.cakeshop.persistence.IngredientRepository;
import uk.gov.justice.services.example.cakeshop.persistence.RecipeRepository;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Ingredient;
import uk.gov.justice.services.example.cakeshop.persistence.entity.Recipe;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RecipeAddedEventListenerTest {

    private static final String INGREDIENT_NAME = "Flour";

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private RecipeAddedToRecipeConverter recipeAddedToRecipeConverter;

    @Mock
    private RecipeAddedToIngredientsConverter recipeAddedToIngredientsConverter;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private RecipeAdded recipeAdded;

    @Mock
    private Recipe recipe;

    @Mock
    private Ingredient ingredient;

    @Mock
    private JsonObject payload;

    @InjectMocks
    private RecipeAddedEventListener recipeAddedEventListener;

    @Before
    public void setup() {
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(payload, RecipeAdded.class)).thenReturn(recipeAdded);
        when(recipeAddedToRecipeConverter.convert(recipeAdded)).thenReturn(recipe);
        when(ingredient.getName()).thenReturn(INGREDIENT_NAME);
        when(recipeAddedToIngredientsConverter.convert(recipeAdded)).thenReturn(singletonList(ingredient));
    }

    @Test
    public void shouldHandleRecipeAddedEvent() throws Exception {
        when(ingredientRepository.findByNameIgnoreCase(INGREDIENT_NAME)).thenReturn(emptyList());

        recipeAddedEventListener.recipeAdded(envelope);

        verify(recipeRepository).save(recipe);
        verify(ingredientRepository).save(ingredient);
    }

    @Test
    public void shouldHandleRecipeAddedEventWithExistingIngredient() throws Exception {
        when(ingredientRepository.findByNameIgnoreCase(INGREDIENT_NAME)).thenReturn(singletonList(ingredient));

        recipeAddedEventListener.recipeAdded(envelope);

        verify(recipeRepository).save(recipe);
        verify(ingredientRepository, never()).save(ingredient);
    }
}