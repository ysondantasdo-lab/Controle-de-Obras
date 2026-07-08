package br.com.yson.controle.de.obras.data.repository

import br.com.yson.controle.de.obras.data.dao.AppDao
import br.com.yson.controle.de.obras.data.model.*
import kotlinx.coroutines.flow.Flow
import java.lang.StringBuilder

class AppRepository(private val appDao: AppDao) {

    // --- Fornecedores ---
    val allFornecedores: Flow<List<Fornecedor>> = appDao.getAllFornecedores()
    
    suspend fun getFornecedorById(id: Int): Fornecedor? = appDao.getFornecedorById(id)
    
    suspend fun insertFornecedor(fornecedor: Fornecedor): Long = appDao.insertFornecedor(fornecedor)
    
    suspend fun updateFornecedor(fornecedor: Fornecedor) = appDao.updateFornecedor(fornecedor)
    
    suspend fun deleteFornecedor(fornecedor: Fornecedor) = appDao.deleteFornecedor(fornecedor)

    // --- Prestadores ---
    val allPrestadores: Flow<List<Prestador>> = appDao.getAllPrestadores()
    
    suspend fun getPrestadorById(id: Int): Prestador? = appDao.getPrestadorById(id)
    
    suspend fun insertPrestador(prestador: Prestador): Long = appDao.insertPrestador(prestador)
    
    suspend fun updatePrestador(prestador: Prestador) = appDao.updatePrestador(prestador)
    
    suspend fun deletePrestador(prestador: Prestador) = appDao.deletePrestador(prestador)

    // --- Obras ---
    val allObras: Flow<List<Obra>> = appDao.getAllObras()
    
    suspend fun getObraById(id: Int): Obra? = appDao.getObraById(id)
    
    suspend fun insertObra(obra: Obra): Long = appDao.insertObra(obra)

    suspend fun deleteObra(obra: Obra) = appDao.deleteObra(obra)

    // --- Fornecedores da Obra ---
    fun getFornecedoresForObra(obraId: Int): Flow<List<FornecedorObraWithDetails>> = 
        appDao.getFornecedoresForObra(obraId)

    suspend fun insertFornecedorObra(fornecedorObra: FornecedorObra): Long = 
        appDao.insertFornecedorObra(fornecedorObra)

    suspend fun deleteFornecedorObra(fornecedorObra: FornecedorObra) =
        appDao.deleteFornecedorObra(fornecedorObra)

    fun getPagamentosForFornecedorObra(fornecedorObraId: Int): Flow<List<PagamentoFornecedor>> = 
        appDao.getPagamentosForFornecedorObra(fornecedorObraId)

    suspend fun getHistoricalTotalPaidForFornecedor(fornecedorId: Int): Double =
        appDao.getTotalPaidForFornecedor(fornecedorId)

    suspend fun insertPagamentoFornecedor(pagamento: PagamentoFornecedor): Long = 
        appDao.insertPagamentoFornecedor(pagamento)

    suspend fun deletePagamentoFornecedor(pagamento: PagamentoFornecedor) =
        appDao.deletePagamentoFornecedor(pagamento)

    // --- Prestadores da Obra ---
    fun getPrestadoresForObra(obraId: Int): Flow<List<PrestadorObraWithDetails>> = 
        appDao.getPrestadoresForObra(obraId)

    suspend fun insertPrestadorObra(prestadorObra: PrestadorObra): Long = 
        appDao.insertPrestadorObra(prestadorObra)

    suspend fun deletePrestadorObra(prestadorObra: PrestadorObra) =
        appDao.deletePrestadorObra(prestadorObra)

    fun getPagamentosForPrestadorObra(prestadorObraId: Int): Flow<List<PagamentoPrestador>> = 
        appDao.getPagamentosForPrestadorObra(prestadorObraId)

    suspend fun insertPagamentoPrestador(pagamento: PagamentoPrestador): Long = 
        appDao.insertPagamentoPrestador(pagamento)

    suspend fun deletePagamentoPrestador(pagamento: PagamentoPrestador) =
        appDao.deletePagamentoPrestador(pagamento)


    // --- Lotes da Obra ---
    fun getLotesForObra(obraId: Int): Flow<List<LoteObra>> = 
        appDao.getLotesForObra(obraId)

    suspend fun insertLoteObra(loteObra: LoteObra): Long = 
        appDao.insertLoteObra(loteObra)

    suspend fun deleteLoteObra(loteObra: LoteObra) =
        appDao.deleteLoteObra(loteObra)

    // --- Burocracia da Obra ---
    fun getBurocraciasForObra(obraId: Int): Flow<List<BurocraciaObra>> = 
        appDao.getBurocraciasForObra(obraId)

    suspend fun insertBurocraciaObra(burocracia: BurocraciaObra): Long = 
        appDao.insertBurocraciaObra(burocracia)

    suspend fun deleteBurocraciaObra(burocracia: BurocraciaObra) =
        appDao.deleteBurocraciaObra(burocracia)

    // --- CSV IMPORT / EXPORT UTILS ---
    
    fun exportToCsv(fornecedores: List<Fornecedor>, prestadores: List<Prestador>): String {
        val builder = StringBuilder()
        builder.append("tipo,nome,telefone,observacoes\n")
        
        for (f in fornecedores) {
            val safeName = escapeField(f.nome)
            val safePhone = escapeField(f.telefone)
            val safeNotes = escapeField(f.observacoes)
            builder.append("FORNECEDOR,$safeName,$safePhone,$safeNotes\n")
        }
        
        for (p in prestadores) {
            val safeName = escapeField(p.nome)
            val safePhone = escapeField(p.telefone)
            val safeNotes = escapeField(p.observacoes)
            builder.append("PRESTADOR,$safeName,$safePhone,$safeNotes\n")
        }
        
        return builder.toString()
    }

    suspend fun importFromCsv(csvContent: String): Int {
        val lines = csvContent.lines()
        var importedCount = 0
        
        for (line in lines) {
            if (line.isBlank()) continue
            // Skip header if matches
            if (line.startsWith("tipo,nome", ignoreCase = true)) continue
            
            val parts = line.split(",")
            if (parts.size >= 4) {
                val tipo = parts[0].trim().uppercase()
                val nome = unescapeField(parts[1])
                val telefone = unescapeField(parts[2])
                // Combine remaining parts in case notes had commas
                val observacoesUnescaped = parts.subList(3, parts.size).joinToString(",")
                val observacoes = unescapeField(observacoesUnescaped)
                
                if (tipo == "FORNECEDOR") {
                    insertFornecedor(Fornecedor(nome = nome, telefone = telefone, observacoes = observacoes))
                    importedCount++
                } else if (tipo == "PRESTADOR") {
                    insertPrestador(Prestador(nome = nome, telefone = telefone, observacoes = observacoes))
                    importedCount++
                }
            }
        }
        return importedCount
    }

    private fun escapeField(value: String): String {
        return value.replace("\n", " ").replace("\r", " ").replace(",", ";")
    }

    private fun unescapeField(value: String): String {
        return value.trim().replace(";", ",")
    }
}
