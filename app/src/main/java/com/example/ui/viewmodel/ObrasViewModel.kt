package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ObrasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    val allFornecedores: StateFlow<List<Fornecedor>>
    val allPrestadores: StateFlow<List<Prestador>>
    val allObras: StateFlow<List<Obra>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database.appDao())

        allFornecedores = repository.allFornecedores
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allPrestadores = repository.allPrestadores
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allObras = repository.allObras
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // --- FORNECEDORES ---
    fun insertFornecedor(nome: String, telefone: String, observacoes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFornecedor(Fornecedor(nome = nome, telefone = telefone, observacoes = observacoes))
        }
    }

    fun updateFornecedor(fornecedor: Fornecedor) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateFornecedor(fornecedor)
        }
    }

    fun deleteFornecedor(fornecedor: Fornecedor) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFornecedor(fornecedor)
        }
    }

    // --- PRESTADORES ---
    fun insertPrestador(nome: String, telefone: String, observacoes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPrestador(Prestador(nome = nome, telefone = telefone, observacoes = observacoes))
        }
    }

    fun updatePrestador(prestador: Prestador) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePrestador(prestador)
        }
    }

    fun deletePrestador(prestador: Prestador) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePrestador(prestador)
        }
    }

    // --- OBRAS ---
    fun insertObra(nome: String, endereco: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertObra(Obra(nome = nome, endereco = endereco))
        }
    }

    fun deleteObra(obra: Obra) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteObra(obra)
        }
    }

    // --- COORDENATED FORNECEDORES DA OBRA ---
    fun getFornecedoresForObra(obraId: Int): Flow<List<FornecedorObraWithDetails>> {
        return repository.getFornecedoresForObra(obraId)
    }

    fun launchFornecedorObra(
        obraId: Int,
        fornecedorId: Int,
        valorServico: Double,
        valorPago: Double,
        dataPagamento: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val recordId = repository.insertFornecedorObra(
                FornecedorObra(
                    obraId = obraId,
                    fornecedorId = fornecedorId,
                    valorServico = valorServico,
                    valorPago = valorPago,
                    dataPagamento = dataPagamento
                )
            ).toInt()

            // If an initial payment amount was declared, record it in history too
            if (valorPago > 0.0) {
                repository.insertPagamentoFornecedor(
                    PagamentoFornecedor(
                        fornecedorObraId = recordId,
                        valor = valorPago,
                        data = dataPagamento
                    )
                )
            }
        }
    }

    fun getPagamentosForFornecedorObra(fornecedorObraId: Int): Flow<List<PagamentoFornecedor>> {
        return repository.getPagamentosForFornecedorObra(fornecedorObraId)
    }

    fun deleteFornecedorObra(fornecedorObra: FornecedorObraWithDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFornecedorObra(
                FornecedorObra(
                    id = fornecedorObra.id,
                    obraId = fornecedorObra.obraId,
                    fornecedorId = fornecedorObra.fornecedorId,
                    valorServico = fornecedorObra.valorServico,
                    valorPago = fornecedorObra.valorPago,
                    dataPagamento = fornecedorObra.dataPagamento
                )
            )
        }
    }

    fun deletePagamentoFornecedor(pagamento: PagamentoFornecedor, fornecedorObra: FornecedorObraWithDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePagamentoFornecedor(pagamento)
            val updatedPayments = repository.getPagamentosForFornecedorObra(fornecedorObra.id).first()
            val totalSum = updatedPayments.sumOf { it.valor }
            repository.insertFornecedorObra(
                FornecedorObra(
                    id = fornecedorObra.id,
                    obraId = fornecedorObra.obraId,
                    fornecedorId = fornecedorObra.fornecedorId,
                    valorServico = fornecedorObra.valorServico,
                    valorPago = totalSum,
                    dataPagamento = fornecedorObra.dataPagamento
                )
            )
        }
    }

    suspend fun getHistoricalTotalPaidForFornecedor(fornecedorId: Int): Double {
        return repository.getHistoricalTotalPaidForFornecedor(fornecedorId)
    }

    fun launchPagamentoFornecedor(
        fornecedorObra: FornecedorObraWithDetails,
        valor: Double,
        data: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert history entry
            repository.insertPagamentoFornecedor(
                PagamentoFornecedor(
                    fornecedorObraId = fornecedorObra.id,
                    valor = valor,
                    data = data
                )
            )

            // 2. Query all payments to calculate exact sum
            val updatedPayments = repository.getPagamentosForFornecedorObra(fornecedorObra.id).first()
            val totalSum = updatedPayments.sumOf { it.valor }

            // 3. Update the parent link row with updated cumulative sum and latest payment date
            repository.insertFornecedorObra(
                FornecedorObra(
                    id = fornecedorObra.id,
                    obraId = fornecedorObra.obraId,
                    fornecedorId = fornecedorObra.fornecedorId,
                    valorServico = fornecedorObra.valorServico,
                    valorPago = totalSum,
                    dataPagamento = data
                )
            )
        }
    }


    // --- COORDENATED PRESTADORES DA OBRA ---
    fun getPrestadoresForObra(obraId: Int): Flow<List<PrestadorObraWithDetails>> {
        return repository.getPrestadoresForObra(obraId)
    }

    fun launchPrestadorObra(
        obraId: Int,
        prestadorId: Int,
        valorMaterial: Double,
        valorPago: Double,
        dataPagamento: String,
        existingId: Int? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val recordId = repository.insertPrestadorObra(
                PrestadorObra(
                    id = existingId ?: 0,
                    obraId = obraId,
                    prestadorId = prestadorId,
                    valorMaterial = valorMaterial,
                    valorPago = if (existingId != null) {
                        val existing = repository.getPrestadoresForObra(obraId).first().find { it.id == existingId }
                        existing?.valorPago ?: 0.0
                    } else {
                        valorPago
                    },
                    dataPagamento = dataPagamento
                )
            ).toInt()

            // If an initial payment amount was declared, record it in history too
            if (valorPago > 0.0) {
                repository.insertPagamentoPrestador(
                    PagamentoPrestador(
                        prestadorObraId = recordId,
                        valor = valorPago,
                        data = dataPagamento
                    )
                )

                // Recalculate total sum to keep DB strictly in-sync and accurate
                val updatedPayments = repository.getPagamentosForPrestadorObra(recordId).first()
                val totalSum = updatedPayments.sumOf { it.valor }

                repository.insertPrestadorObra(
                    PrestadorObra(
                        id = recordId,
                        obraId = obraId,
                        prestadorId = prestadorId,
                        valorMaterial = valorMaterial,
                        valorPago = totalSum,
                        dataPagamento = dataPagamento
                    )
                )
            }
        }
    }

    fun getPagamentosForPrestadorObra(prestadorObraId: Int): Flow<List<PagamentoPrestador>> {
        return repository.getPagamentosForPrestadorObra(prestadorObraId)
    }

    fun deletePrestadorObra(prestadorObra: PrestadorObraWithDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePrestadorObra(
                PrestadorObra(
                    id = prestadorObra.id,
                    obraId = prestadorObra.obraId,
                    prestadorId = prestadorObra.prestadorId,
                    valorMaterial = prestadorObra.valorMaterial,
                    valorPago = prestadorObra.valorPago,
                    dataPagamento = prestadorObra.dataPagamento
                )
            )
        }
    }

    fun deletePagamentoPrestador(pagamento: PagamentoPrestador, prestadorObra: PrestadorObraWithDetails) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePagamentoPrestador(pagamento)
            val updatedPayments = repository.getPagamentosForPrestadorObra(prestadorObra.id).first()
            val totalSum = updatedPayments.sumOf { it.valor }
            repository.insertPrestadorObra(
                PrestadorObra(
                    id = prestadorObra.id,
                    obraId = prestadorObra.obraId,
                    prestadorId = prestadorObra.prestadorId,
                    valorMaterial = prestadorObra.valorMaterial,
                    valorPago = totalSum,
                    dataPagamento = prestadorObra.dataPagamento
                )
            )
        }
    }

    fun launchPagamentoPrestador(
        prestadorObra: PrestadorObraWithDetails,
        valor: Double,
        data: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Insert history entry
            repository.insertPagamentoPrestador(
                PagamentoPrestador(
                    prestadorObraId = prestadorObra.id,
                    valor = valor,
                    data = data
                )
            )

            // 2. Query all payments to calculate exact sum
            val updatedPayments = repository.getPagamentosForPrestadorObra(prestadorObra.id).first()
            val totalSum = updatedPayments.sumOf { it.valor }

            // 3. Update the parent link row with updated cumulative sum and latest payment date
            repository.insertPrestadorObra(
                PrestadorObra(
                    id = prestadorObra.id,
                    obraId = prestadorObra.obraId,
                    prestadorId = prestadorObra.prestadorId,
                    valorMaterial = prestadorObra.valorMaterial,
                    valorPago = totalSum,
                    dataPagamento = data
                )
            )
        }
    }

    fun updateValorContratadoPrestador(
        prestadorObraId: Int,
        obraId: Int,
        prestadorId: Int,
        nuevoValorContratado: Double,
        dataPagamento: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPayments = repository.getPagamentosForPrestadorObra(prestadorObraId).first()
            val totalSum = updatedPayments.sumOf { it.valor }

            repository.insertPrestadorObra(
                PrestadorObra(
                    id = prestadorObraId,
                    obraId = obraId,
                    prestadorId = prestadorId,
                    valorMaterial = nuevoValorContratado,
                    valorPago = totalSum,
                    dataPagamento = dataPagamento
                )
            )
        }
    }


    // --- LOTES DA OBRA ---
    fun getLotesForObra(obraId: Int): Flow<List<LoteObra>> {
        return repository.getLotesForObra(obraId)
    }

    fun insertLoteObra(obraId: Int, nome: String, tamanhoM2: Int, valor: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLoteObra(
                LoteObra(
                    obraId = obraId,
                    nome = nome,
                    tamanhoM2 = tamanhoM2,
                    valor = valor
                )
            )
        }
    }

    fun deleteLoteObra(loteObra: LoteObra) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLoteObra(loteObra)
        }
    }

    // --- BUROCRACIA DA OBRA ---
    fun getBurocraciasForObra(obraId: Int): Flow<List<BurocraciaObra>> {
        return repository.getBurocraciasForObra(obraId)
    }

    fun insertBurocraciaObra(obraId: Int, nome: String, valorPago: Double, dataControle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBurocraciaObra(
                BurocraciaObra(
                    obraId = obraId,
                    nome = nome,
                    valorPago = valorPago,
                    dataControle = dataControle
                )
            )
        }
    }

    fun deleteBurocraciaObra(burocracia: BurocraciaObra) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBurocraciaObra(burocracia)
        }
    }

    // --- CSV IMPORT & EXPORT EXPOSURE ---
    
    suspend fun getCsvContentForExport(): String {
        return withContext(Dispatchers.IO) {
            val f = allFornecedores.value
            val p = allPrestadores.value
            repository.exportToCsv(f, p)
        }
    }

    suspend fun importCsvContent(csvContent: String): Int {
        return withContext(Dispatchers.IO) {
            repository.importFromCsv(csvContent)
        }
    }
}
