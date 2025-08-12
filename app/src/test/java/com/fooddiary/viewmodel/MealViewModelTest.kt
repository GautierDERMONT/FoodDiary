package com.fooddiary.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fooddiary.data.AppDatabase
import com.fooddiary.data.MealDao
import com.fooddiary.data.MealEntity
import com.fooddiary.model.Meal
import com.fooddiary.model.MealType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.eq

@ExperimentalCoroutinesApi
@RunWith(JUnit4::class)
class MealViewModelTest {

    // Règle JUnit qui fait que les LiveData/StateFlow s'exécutent immédiatement dans les tests
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Dispatcher de test pour contrôler l'exécution des coroutines dans nos tests
    private val testDispatcher = StandardTestDispatcher()

    // Objets "faux" (mocks) qui remplacent les vraies classes pour les tests
    @Mock
    private lateinit var mockDatabase: AppDatabase

    @Mock
    private lateinit var mockMealDao: MealDao

    @Mock
    private lateinit var mockApplication: android.app.Application

    @Mock
    private lateinit var mockPrefs: android.content.SharedPreferences

    @Mock
    private lateinit var mockPrefsEditor: android.content.SharedPreferences.Editor


    private lateinit var viewModel: MealViewModel

    @Before
    fun setup() {

        Dispatchers.setMain(testDispatcher)


        MockitoAnnotations.openMocks(this)


        whenever(mockDatabase.mealDao()).thenReturn(mockMealDao)


        whenever(mockMealDao.getAllMeals()).thenReturn(flowOf(emptyList()))
        whenever(mockMealDao.getMealsByDayAndWeek(any(), any())).thenReturn(flowOf(emptyList()))


        whenever(mockApplication.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        whenever(mockPrefs.getString(any(), any())).thenReturn("") // Email vide par défaut
        whenever(mockPrefs.edit()).thenReturn(mockPrefsEditor)
        whenever(mockPrefsEditor.putString(any(), any())).thenReturn(mockPrefsEditor)
    }

    @After
    fun tearDown() {

        Dispatchers.resetMain()


        reset(mockMealDao, mockApplication, mockPrefs, mockPrefsEditor)
    }

    @Test
    fun `changeWeekOffset should update currentWeekOffset within bounds`() = runTest(testDispatcher) {
        // TEST : Vérifier que le changement de semaine respecte les limites

        // On crée notre ViewModel avec nos objets factices
        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle() // On attend que toutes les coroutines se terminent

        // Test 1 : On recule d'une semaine
        viewModel.changeWeekOffset(-1)
        Assert.assertEquals("La navigation vers la semaine précédente fonctionne correctement",
            -1, viewModel.currentWeekOffset.value)

        // Test 2 : On essaie de reculer de 10 semaines (trop !), ça doit être limité à -5
        viewModel.changeWeekOffset(-10)
        Assert.assertEquals("La limitation du nombre de semaines précédentes est respectée (max 5 semaines)",
            -MealViewModel.MAX_WEEK_OFFSET, viewModel.currentWeekOffset.value)

        // Test 3 : On revient à la semaine actuelle
        viewModel.changeWeekOffset(1)
        Assert.assertEquals("Le retour à la semaine courante fonctionne parfaitement",
            0, viewModel.currentWeekOffset.value)

        println("TEST RÉUSSI : La navigation entre les semaines respecte bien toutes les limites")
    }

    @Test
    fun `updateMeal should insert new meal if not exists`() = runTest(testDispatcher) {
        // TEST : Vérifier qu'un nouveau repas est ajouté s'il n'existe pas déjà

        // On configure notre DAO factice pour retourner une liste vide
        // (= pas de repas existant)
        whenever(mockMealDao.getMealsByDayAndWeek(any(), any()))
            .thenReturn(flowOf(emptyList()))

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle() // On attend l'initialisation

        // On crée un repas de test
        val testMeal = Meal(
            type = MealType.LUNCH,
            description = "Test meal",
            mealIndex = 1,
            day = "Lun"
        )

        // On demande au ViewModel de mettre à jour ce repas
        viewModel.updateMeal("Lun", 1, testMeal)
        advanceUntilIdle() // On attend que l'opération se termine

        // On vérifie que le DAO a bien été appelé pour INSÉRER un nouveau repas
        verify(mockMealDao).insert(any())

        println("TEST RÉUSSI : Un nouveau repas peut bien être ajouté quand il n'existe pas encore")
    }

    @Test
    fun `updateMeal should update existing meal`() = runTest(testDispatcher) {
        // TEST : Vérifier qu'un repas existant est modifié (pas ajouté)

        // On crée un repas qui existe déjà dans la base
        val existingMeal = MealEntity(
            day = "Lun",
            mealIndex = 1,
            type = MealType.LUNCH,
            description = "Old description",
            photoUri = null,
            notes = null,
            weekNumber = 1
        )

        // On configure notre DAO pour retourner ce repas existant
        whenever(mockMealDao.getMealsByDayAndWeek(any(), any()))
            .thenReturn(flowOf(listOf(existingMeal)))

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle()

        // On crée une version mise à jour du repas
        val updatedMeal = Meal(
            type = MealType.LUNCH,
            description = "Updated description", // Description changée
            mealIndex = 1,
            day = "Lun"
        )

        // On demande la mise à jour
        viewModel.updateMeal("Lun", 1, updatedMeal)
        advanceUntilIdle()

        // On vérifie que le DAO a bien été appelé pour MODIFIER (pas insérer)
        verify(mockMealDao).update(any())

        println("TEST RÉUSSI : Un repas existant peut bien se modifier sans créer de doublon")
    }

    @Test
    fun `deleteMeal should call dao with correct parameters`() = runTest(testDispatcher) {
        // TEST : Vérifier que la suppression d'un repas appelle bien le DAO

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle()

        // On supprime le repas du mardi, index 2
        viewModel.deleteMeal("Mar", 2)
        advanceUntilIdle()

        // On vérifie que le DAO a été appelé avec les bons paramètres
        verify(mockMealDao).deleteMealAt(eq("Mar"), any(), eq(2))

        println("TEST RÉUSSI : La suppression d'un repas spécifique fonctionne parfaitement")
    }

    @Test
    fun `clearCurrentWeekData should delete meals for current week`() = runTest(testDispatcher) {
        // TEST : Vérifier que l'effacement des données de la semaine fonctionne

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle()

        // On demande l'effacement de la semaine courante
        viewModel.clearCurrentWeekData()
        advanceUntilIdle()

        // On vérifie que le DAO a été appelé pour supprimer les repas de la semaine
        verify(mockMealDao).deleteMealsByWeekNumber(any())

        println("TEST RÉUSSI : L'effacement complet d'une semaine fonctionne comme prévu")
    }

    @Test
    fun `addEmptyMeal should not add meal if limit reached`() = runTest(testDispatcher) {
        // TEST : Vérifier qu'on ne peut pas ajouter de repas si on a atteint la limite (8 repas)

        // On crée 8 repas existants (indices 0 à 7) pour atteindre la limite
        val existingMeals = (0..7).map {
            MealEntity(
                day = "Lun",
                mealIndex = it,
                type = MealType.CUSTOM,
                description = if (it < 3) "Meal $it" else "", // Les 3 premiers ont une description
                photoUri = null,
                notes = null,
                weekNumber = 1
            )
        }

        // On configure le DAO pour retourner ces 8 repas
        whenever(mockMealDao.getMealsByDayAndWeek(any(), any()))
            .thenReturn(flowOf(existingMeals))

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle()

        // On essaie d'ajouter un repas vide
        viewModel.addEmptyMeal("Lun")
        advanceUntilIdle()

        // On vérifie qu'AUCUN repas n'a été inséré (car limite atteinte)
        verify(mockMealDao, never()).insert(any())

        println("TEST RÉUSSI : La limitation à 8 repas maximum par jour est bien respectée")
    }

    @Test
    fun `addEmptyMeal should add meal if under limit and no empty meal exists`() = runTest(testDispatcher) {
        // TEST : Vérifier qu'on peut ajouter un repas vide si on est sous la limite

        // On crée seulement 1 repas existant (bien en-dessous de la limite de 8)
        val existingMeals = listOf(
            MealEntity(
                day = "Lun",
                mealIndex = 0,
                type = MealType.BREAKFAST,
                description = "Breakfast", // Ce repas a une description (pas vide)
                photoUri = null,
                notes = null,
                weekNumber = 1
            )
        )

        whenever(mockMealDao.getMealsByDayAndWeek(any(), any()))
            .thenReturn(flowOf(existingMeals))

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle()

        // On essaie d'ajouter un repas vide
        viewModel.addEmptyMeal("Lun")
        advanceUntilIdle()

        // Cette fois, un repas DOIT être inséré (on est sous la limite et pas de repas vide existant)
        verify(mockMealDao).insert(any())

        println("TEST RÉUSSI : On peut bien ajouter un repas vide quand on n'a pas atteint la limite")
    }

    @Test
    fun `removeLastViergeMeal should delete last empty meal`() = runTest(testDispatcher) {
        // TEST : Vérifier que la suppression du dernier repas vide fonctionne

        // On crée un repas vide (vierge) à l'index 4
        val viergeMeal = MealEntity(
            day = "Lun",
            mealIndex = 4,
            type = MealType.CUSTOM,
            description = "", // Description vide = repas vierge
            photoUri = null,  // Pas de photo = repas vierge
            notes = null,
            weekNumber = 1
        )

        whenever(mockMealDao.getMealsByDayAndWeek(any(), any()))
            .thenReturn(flowOf(listOf(viergeMeal)))

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle()

        // On demande la suppression du dernier repas vierge
        viewModel.removeLastViergeMeal("Lun")
        advanceUntilIdle()

        // On vérifie que ce repas précis a été supprimé
        verify(mockMealDao).delete(viergeMeal)

        println("TEST RÉUSSI : La suppression du dernier repas vide fonctionne")
    }

    @Test
    fun `updateDieticianEmail should update email in preferences`() = runTest(testDispatcher) {
        // TEST : Vérifier que la mise à jour de l'email du diététicien fonctionne

        viewModel = MealViewModel(mockDatabase, mockApplication)
        advanceUntilIdle()

        // On met à jour l'email
        val testEmail = "test@example.com"
        viewModel.updateDieticianEmail(testEmail)
        advanceUntilIdle()

        // On vérifie que :
        // 1. La valeur dans le ViewModel a changé
        Assert.assertEquals("L'email du diététicien est bien mis à jour dans le ViewModel",
            testEmail, viewModel.dieticianEmail.value)

        // 2. L'email a été sauvegardé dans les préférences
        verify(mockPrefsEditor).putString("dietician_email", testEmail)
        verify(mockPrefsEditor).apply() // La sauvegarde a été appliquée

        println("TEST RÉUSSI : L'email de la diététicienne se sauvegarde correctement dans les préférences")
    }
}